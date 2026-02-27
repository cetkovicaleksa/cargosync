package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductSearchFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.SaveProductRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.service.ProductService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Factory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Product;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(@RequestBody @Valid SaveProductRequest request) {
        return mapToDto(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ProductDto updateProduct(@PathVariable UUID id, @RequestBody @Valid SaveProductRequest request) {
        return mapToDto(productService.updateProduct(id, request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<ProductDto> searchProducts(ProductSearchFilter filter, Pageable pageable) {
        return productService.searchProducts(filter, pageable).map(this::mapToDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
    }

    private ProductDto mapToDto(Product product) {
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
                product.getVersion()
        );
    }
}