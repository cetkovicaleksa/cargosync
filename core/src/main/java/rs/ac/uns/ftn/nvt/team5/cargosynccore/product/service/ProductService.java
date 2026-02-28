package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductSearchFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.SaveProductRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.repository.ProductRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Factory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Product;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.ProductCategory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.FactoryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.ProductCategoryRepository;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final FactoryRepository factoryRepository;

    @Transactional
    public Product createProduct(SaveProductRequest request) {
        Product product = new Product();
        return saveOrUpdate(product, request);
    }

    @Transactional
    public Product updateProduct(UUID id, SaveProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        if (request.version() != null) {
            product.setVersion(request.version());
        }

        return saveOrUpdate(product, request);
    }

    private Product saveOrUpdate(Product product, SaveProductRequest request)
            throws OptimisticLockingFailureException {
        String normalizedCatName = request.categoryName().trim();
        ProductCategory category = categoryRepository.findByNameIgnoreCase(normalizedCatName)
                .orElseGet(() -> {
                    ProductCategory newCat = new ProductCategory();
                    newCat.setName(normalizedCatName);
                    return categoryRepository.save(newCat);
                });

        product.setName(request.name());
        product.setImageUrl(request.imageUrl());
        product.setPrice(request.price());
        product.setWeight(request.weight());
        product.setForSale(request.isForSale());
        product.setCategory(category);

        if (request.factoryIds() != null && !request.factoryIds().isEmpty()) {
            List<Factory> factories = factoryRepository.findAllById(request.factoryIds());
            product.setFactories(new HashSet<>(factories));
        } else {
            product.setFactories(new HashSet<>());
        }

        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchProducts(ProductSearchFilter filter, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> cb.isTrue(cb.literal(true));

        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%"));
        }
        if (filter.categoryName() != null && !filter.categoryName().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("category").get("name")), "%" + filter.categoryName().toLowerCase() + "%"));
        }
        if (filter.minPrice() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("price"), filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("price"), filter.maxPrice()));
        }
        if (filter.isForSale() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isForSale"), filter.isForSale()));
        }

        return productRepository.findAll(spec, pageable);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        productRepository.deleteById(id);
    }
}