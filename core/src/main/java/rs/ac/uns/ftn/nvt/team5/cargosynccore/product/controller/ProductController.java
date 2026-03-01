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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(
            @RequestPart("data") @Valid SaveProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ProductMapper.mapToDto(productService.createProduct(request, image));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    public ProductDto updateProduct(
            @PathVariable UUID id,
            @RequestPart("data") @Valid SaveProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ProductMapper.mapToDto(productService.updateProduct(id, request, image));
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