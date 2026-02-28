package rs.ac.uns.ftn.nvt.team5.cargosynccore.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.dto.CreateOrderRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.dto.OrderDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.dto.OrderItemDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.model.Order;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.model.OrderItem;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.service.OrderService;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto createOrder(
            @AuthenticationPrincipal Account account,
            @RequestBody @Valid CreateOrderRequest request
    ) {
        return mapToDto(orderService.createOrder(account, request));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Page<OrderDto> getMyOrders(
            @AuthenticationPrincipal Account account,
            Pageable pageable
    ) {
        return orderService.getMyOrders(account, pageable).map(this::mapToDto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderDto getMyOrder(
            @AuthenticationPrincipal Account account,
            @PathVariable UUID id
    ) {
        return mapToDto(orderService.getMyOrder(account, id));
    }

    private OrderDto mapToDto(Order order) {
        var items = order.getItems().stream().map(this::mapItemToDto).toList();
        return new OrderDto(
                order.getId(),
                order.getCompany().getId(),
                order.getCompany().getName(),
                order.getCompany().getAddress(),
                order.getStatus(),
                order.getTotalAmount(),
                items,
                order.getCreatedDate()
        );
    }

    private OrderItemDto mapItemToDto(OrderItem item) {
        return new OrderItemDto(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getImageUrl(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity()))
        );
    }
}
