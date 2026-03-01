package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductSearchFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.SaveProductRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.repository.ProductRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.Factory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.BadRequestException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Product;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.ProductCategory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository.FactoryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.ProductCategoryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.ContentStore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final FactoryRepository factoryRepository;
    private final ContentStore contentStore;

    @Transactional
    public Product createProduct(SaveProductRequest request, MultipartFile image) {
        Product product = new Product();
        return saveOrUpdate(product, request, image);
    }

    @Transactional
    public Product updateProduct(UUID id, SaveProductRequest request, MultipartFile image) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        if (request.version() != null) {
            product.setVersion(request.version());
        }

        return saveOrUpdate(product, request, image);
    }

    private Product saveOrUpdate(Product product, SaveProductRequest request, MultipartFile image)
            throws OptimisticLockingFailureException {
        String normalizedCatName = request.categoryName().trim();
        ProductCategory category = categoryRepository.findByNameIgnoreCase(normalizedCatName)
                .orElseGet(() -> {
                    ProductCategory newCat = new ProductCategory();
                    newCat.setName(normalizedCatName);
                    return categoryRepository.save(newCat);
                });

        product.setName(request.name());
        product.setImageUrl(resolveImageUrl(request.imageUrl(), image));
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

    private String resolveImageUrl(String currentImageUrl, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return currentImageUrl;
        }

        validateImage(image);
        return saveImage(image);
    }

    private void validateImage(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BadRequestException("Image must be a JPG, PNG, or WebP image");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new BadRequestException("Image must not exceed 10 MB");
        }
    }

    private String saveImage(MultipartFile image) {
        String ext = extensionOrDefault(image.getOriginalFilename(), "jpg");
        String path = "products/" + UUID.randomUUID() + "." + ext;
        contentStore.save(path, image);
        return path;
    }

    private String extensionOrDefault(String originalFilename, String fallback) {
        if (originalFilename == null) {
            return fallback;
        }

        int index = originalFilename.lastIndexOf('.');
        if (index < 0 || index == originalFilename.length() - 1) {
            return fallback;
        }

        return originalFilename.substring(index + 1).toLowerCase();
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