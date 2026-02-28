package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductMapper;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductSearchFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.SaveProductRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.service.ProductService;

import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(@RequestBody @Valid SaveProductRequest request) {
        return ProductMapper.mapToDto(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ProductDto updateProduct(@PathVariable UUID id, @RequestBody @Valid SaveProductRequest request) {
        return ProductMapper.mapToDto(productService.updateProduct(id, request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<ProductDto> searchProducts(ProductSearchFilter filter, Pageable pageable) {
        return productService.searchProducts(filter, pageable).map(ProductMapper::mapToDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
    }

}