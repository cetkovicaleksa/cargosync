package rs.ac.uns.ftn.nvt.team5.cargosynccore.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.dto.CreateOrderRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.dto.OrderItemRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.model.Order;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.model.OrderItem;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.repository.OrderRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.repository.ProductRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.BadRequestException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.ForbiddenException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CompanyRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CustomerRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.WarehouseInventoryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.ContentStore;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.EmailService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final WarehouseInventoryRepository inventoryRepository;
    private final InvoiceService invoiceService;
    private final ContentStore contentStore;
    private final EmailService emailService;

    /**
     * Creates an order within a single database transaction.
     *
     * Flow:
     * 1. Resolve customer from authenticated account
     * 2. Resolve destination company (stub — no approval check yet)
     * 3. Sort items by productId for consistent lock ordering (deadlock prevention)
     * 4. For each item: acquire PESSIMISTIC_WRITE lock on inventory rows,
     *    verify stock, deduct greedily across warehouses
     * 5. Persist Order + OrderItems
     * 6. Generate PDF invoice + send email (best-effort)
     */
    @Transactional
    public Order createOrder(Account account, CreateOrderRequest request) {
        Customer customer = resolveCustomer(account);
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new NotFoundException("Company not found: " + request.companyId()));

        // Sort by product UUID string for consistent lock ordering → prevents deadlocks
        List<OrderItemRequest> sortedItems = request.items().stream()
                .sorted(Comparator.comparing(i -> i.productId().toString()))
                .toList();

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : sortedItems) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new NotFoundException("Product not found: " + itemReq.productId()));

            if (!product.isForSale()) {
                throw new BadRequestException("Product is not available for sale: " + product.getName());
            }

            // Acquire row-level locks on all inventory rows for this product
            List<WarehouseInventory> inventories =
                    inventoryRepository.findByProductIdForUpdate(itemReq.productId());

            long totalStock = inventories.stream().mapToLong(WarehouseInventory::getQuantity).sum();
            if (totalStock < itemReq.quantity()) {
                throw new BadRequestException(
                        "Insufficient stock for '%s'. Requested: %d, Available: %d"
                                .formatted(product.getName(), itemReq.quantity(), totalStock));
            }

            // Deduct greedily: start from warehouse with most stock (sorted DESC)
            long remaining = itemReq.quantity();
            for (WarehouseInventory inv : inventories) {
                if (remaining <= 0) break;
                long deduct = Math.min(inv.getQuantity(), remaining);
                inv.setQuantity(inv.getQuantity() - deduct);
                remaining -= deduct;
            }

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(product.getPrice());
            orderItems.add(item);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setCompany(company);
        order.setTotalAmount(total);
        orderItems.forEach(item -> {
            item.setOrder(order);
            order.getItems().add(item);
        });

        Order saved = orderRepository.save(order);

        // Best-effort: PDF + email failure does NOT roll back the order
        try {
            byte[] pdfBytes = invoiceService.generateInvoicePdf(saved);
            String pdfPath = "invoices/" + saved.getId() + ".pdf";
            contentStore.save(pdfPath, new ByteArrayResource(pdfBytes));
            saved.setInvoicePath(pdfPath);
            orderRepository.save(saved);

            emailService.sendInvoiceEmail(
                    customer.getEmail(),
                    customer.getFirstName(),
                    saved,
                    pdfBytes
            );
        } catch (Exception e) {
            log.error("Failed to generate/send invoice for order {}: {}", saved.getId(), e.getMessage());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Order> getMyOrders(Account account, Pageable pageable) {
        Customer customer = resolveCustomer(account);
        return orderRepository.findByCustomerIdOrderByCreatedDateDesc(customer.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Order getMyOrder(Account account, UUID orderId) {
        Customer customer = resolveCustomer(account);
        return orderRepository.findByIdAndCustomerId(orderId, customer.getId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
    }

    private Customer resolveCustomer(Account account) {
        if (!(account.getOwner() instanceof Customer customer)) {
            throw new ForbiddenException("Only customers can place orders");
        }
        return customer;
    }
}
