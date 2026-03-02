package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryLookupDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryMapper;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.Factory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductSearchFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.SaveProductRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.service.ProductService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Product;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CityRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Products")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CityRepository cityRepository;

    @Operation(summary = "Create product", description = "Creates a new product with optional image. Requires MANAGER role.")
    @ApiResponse(responseCode = "201", description = "Product created")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(
            @RequestPart("data") @Valid SaveProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return mapToDto(productService.createProduct(request, image));
    }

    @Operation(summary = "Update product", description = "Updates an existing product. Requires MANAGER role.")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    public ProductDto updateProduct(
            @PathVariable UUID id,
            @RequestPart("data") @Valid SaveProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return mapToDto(productService.updateProduct(id, request, image));
    }

    @Operation(summary = "Search products (Manager)", description = "Paginated product list for managers — all products including those not for sale. Filter by name, category, price range.")
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public Page<ProductDto> searchProducts(ProductSearchFilter filter, Pageable pageable) {
        return productService.searchProducts(filter, pageable).map(this::mapToDto);
    }

    @Operation(summary = "Product catalogue (Customer)", description = "Paginated catalogue of products available for purchase, enriched with total stock across all warehouses. Results cached in Redis (TTL 5 min). Filter by name, categoryName, minPrice, maxPrice.")
    @GetMapping("/catalogue")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Page<ProductDto> getCatalogue(ProductSearchFilter filter, Pageable pageable) {
        return productService.getCatalogue(filter, pageable).toPage(pageable);
    }

    @Operation(summary = "Delete product", description = "Deletes a product. Requires MANAGER role.")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
    }

    @Operation(summary = "Get product factories", description = "Retrieves a list of factories that produce the specified product.")
    @GetMapping("/{id}/factories")
    @PreAuthorize("hasRole('MANAGER')")
    public List<FactoryDto> getProductFactories(@PathVariable UUID id) {
        return productService.getProductFactories(id)
                .stream().map(f -> FactoryMapper.mapToDto(f, cityRepository))
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get product factories (lookup)", description = "Retrieves a list of factories that produce the specified product, with minimal information.")
    @GetMapping("/{id}/factories/lookup")
    @PreAuthorize("hasRole('MANAGER')")
    public List<FactoryLookupDto> getProductFactoriesLookup(@PathVariable UUID id) {
        return productService.getProductFactories(id)
                .stream().map(f -> FactoryMapper.mapToLookupDto(f, cityRepository))
                .collect(Collectors.toList());
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