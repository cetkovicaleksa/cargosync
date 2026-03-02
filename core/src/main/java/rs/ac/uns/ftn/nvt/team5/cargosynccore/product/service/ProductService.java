package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductSearchFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductSummaryDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.SaveProductRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.repository.ProductRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.Factory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.PageResult;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.BadRequestException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Product;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.ProductCategory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository.FactoryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.ProductCategoryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.ContentStore;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseInventoryRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final WarehouseInventoryRepository inventoryRepository;
    private final ContentStore contentStore;
    private final ModelMapper modelMapper;

    @Caching(evict = {
            @CacheEvict(cacheNames = {"productCategories", "factoryProducts", "catalogue"}, allEntries = true)
    })
    @Transactional
    public Product createProduct(SaveProductRequest request, MultipartFile image) {
        Product product = new Product();
        return saveOrUpdate(product, request, image);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = {"productCategories", "factoryProducts", "catalogue"}, allEntries = true)
    })
    @Transactional
    public Product updateProduct(UUID id, SaveProductRequest request, MultipartFile image) {
        if (request.version() == null) {
            throw new BadRequestException("Version is required for product updates");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        product.setVersion(request.version());

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
        Specification<Product> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (filter.name() != null && !filter.name().isBlank()) {
                String lowerName = filter.name().toLowerCase();
                predicates.add(
                    cb.like(cb.lower(root.get("name")), "%" + lowerName + "%")
                );
            }

            if (filter.categoryName() != null && !filter.categoryName().isBlank()) {
                predicates.add(
                    cb.like(cb.lower(root.get("category").get("name")),
                            "%" + filter.categoryName().toLowerCase() + "%")
                );
            }

            if (filter.minPrice() != null) {
                predicates.add(
                    cb.greaterThanOrEqualTo(root.get("price"), filter.minPrice())
                );
            }

            if (filter.maxPrice() != null) {
                predicates.add(
                    cb.lessThanOrEqualTo(root.get("price"), filter.maxPrice())
                );
            }

            if (filter.isForSale() != null) {
                predicates.add(
                    cb.equal(root.get("isForSale"), filter.isForSale())
                );
            }

            List<Order> orders = new ArrayList<>();

            if (filter.name() != null && !filter.name().isBlank()) {

                String lowerName = filter.name().toLowerCase();
                Expression<String> nameLower = cb.lower(root.get("name"));

                Expression<Object> priority = cb.selectCase()
                        .when(cb.equal(nameLower, lowerName), 0)
                        .when(cb.like(nameLower, lowerName + "%"), 1)
                        .otherwise(2);

                orders.add(cb.asc(priority));
            }


            pageable.getSort().forEach(sortOrder -> {
                Path<Object> path = root.get(sortOrder.getProperty());
                orders.add(sortOrder.isAscending()
                        ? cb.asc(path)
                        : cb.desc(path));
            });

            if (!orders.isEmpty()) {
                query.orderBy(orders);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable);
    }

    /**
     * Customer catalogue: only isForSale products enriched with total stock.
     * Cached per unique filter+page combination (TTL: 5 min).
     * Evicted on any product write or order placement (stock change).
     */
    @Cacheable(
            cacheNames = "catalogue",
            key = "#filter.name + ':' + #filter.categoryName + ':' + #filter.minPrice + ':' + #filter.maxPrice + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort"
    )
    @Transactional(readOnly = true)
    public PageResult<ProductDto> getCatalogue(ProductSearchFilter filter, Pageable pageable) {
        ProductSearchFilter forSaleFilter = new ProductSearchFilter(
                filter.name(), filter.categoryName(), filter.minPrice(), filter.maxPrice(), true
        );
        Page<Product> page = searchProducts(forSaleFilter, pageable);

        List<UUID> ids = page.getContent().stream().map(Product::getId).toList();
        Map<UUID, Long> stockMap = ids.isEmpty() ? Map.of() : inventoryRepository
                .findTotalStockByProductIds(ids).stream()
                .collect(Collectors.toMap(r -> (UUID) r[0], r -> ((Number) r[1]).longValue()));

        Page<ProductDto> dtoPage = page.map(p -> mapToCatalogueDto(p, stockMap.getOrDefault(p.getId(), 0L)));
        return PageResult.of(dtoPage);
    }

    private ProductDto mapToCatalogueDto(Product product, Long totalStock) {
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

    @Cacheable(cacheNames = "productCategories", key = "'all'")
    @Transactional(readOnly = true)
    public List<String> getAllCategoryNames() {
        return categoryRepository.findAll().stream()
                .map(ProductCategory::getName)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "factoryProducts", key = "#factoryId")
    @Transactional(readOnly = true)
    public List<ProductSummaryDto> getProductsByFactoryId(UUID factoryId) {
        Specification<Product> spec = (root, query, cb) -> {
            query.distinct(true);
            Join<Product, Factory> join = root.join("factories");
            return cb.equal(join.get("id"), factoryId);
        };
        return productRepository.findAll(spec).stream()
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Factory> getProductFactories(UUID id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return List.of();
        return product.getFactories().stream().toList();
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = {"productCategories", "catalogue"}, allEntries = true)
    })
    @Transactional
    public void deleteProduct(UUID id) {
        productRepository.deleteById(id);
    }

    private ProductSummaryDto mapToSummaryDto(Product product) {
        return new ProductSummaryDto(
                product.getId(),
                product.getName(),
                product.getCategory().getName(),
                product.getImageUrl()
        );
    }
}