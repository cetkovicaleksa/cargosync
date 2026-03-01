package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.Factory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductSearchFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.SaveProductRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.service.ProductService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Product;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseInventoryRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final WarehouseInventoryRepository inventoryRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(
            @RequestPart("data") @Valid SaveProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return mapToDto(productService.createProduct(request, image));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    public ProductDto updateProduct(
            @PathVariable UUID id,
            @RequestPart("data") @Valid SaveProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return mapToDto(productService.updateProduct(id, request, image));
    }

    /** Manager product management — all products, no stock info. */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public Page<ProductDto> searchProducts(ProductSearchFilter filter, Pageable pageable) {
        return productService.searchProducts(filter, pageable).map(this::mapToDto);
    }

    /**
     * Customer product catalogue — only isForSale products, enriched with
     * total stock across all warehouses (2 queries per page, no N+1).
     *
     * Query params: name, categoryName, minPrice, maxPrice, inStockOnly (bool)
     */
    @GetMapping("/catalogue")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Page<ProductDto> getCatalogue(ProductSearchFilter filter, Pageable pageable) {
        // Force isForSale=true regardless of what caller passes
        ProductSearchFilter forSaleFilter = new ProductSearchFilter(
                filter.name(),
                filter.categoryName(),
                filter.minPrice(),
                filter.maxPrice(),
                true
        );
        Page<Product> page = productService.searchProducts(forSaleFilter, pageable);

        List<UUID> ids = page.getContent().stream().map(Product::getId).toList();
        Map<UUID, Long> stockMap = ids.isEmpty() ? Map.of() : inventoryRepository
                .findTotalStockByProductIds(ids).stream()
                .collect(Collectors.toMap(r -> (UUID) r[0], r -> ((Number) r[1]).longValue()));

        return page.map(p -> mapToDto(p, stockMap.getOrDefault(p.getId(), 0L)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
    }

    private ProductDto mapToDto(Product product) {
        return mapToDto(product, null);
    }

    private ProductDto mapToDto(Product product, Long totalStock) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                product.getWeight(),
                product.isForSale(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getFactories().stream().map(Factory::getId).collect(Collectors.toSet()),
                product.getVersion(),
                totalStock
        );
    }
}