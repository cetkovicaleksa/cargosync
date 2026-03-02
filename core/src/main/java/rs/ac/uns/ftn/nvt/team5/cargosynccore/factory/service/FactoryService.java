package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactorySearchFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.SaveFactoryRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.BadRequestException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.City;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.Factory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.FactoryStatus;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.GeoLocation;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CityRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository.FactoryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository.FactoryStatusRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.ContentStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FactoryService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final FactoryRepository factoryRepository;
    private final FactoryStatusRepository factoryStatusRepository;
    private final CityRepository cityRepository;
    private final ContentStore contentStore;

    @Transactional
    public Factory createFactory(SaveFactoryRequest request, List<MultipartFile> images) {
        Factory factory = new Factory();
        Factory savedFactory = saveOrUpdate(factory, request, images);

        FactoryStatus status = new FactoryStatus(savedFactory);
        factoryStatusRepository.save(status);

        return savedFactory;
    }

    @CachePut(value = "factories", key = "#id")
    @Transactional
    public Factory updateFactory(UUID id, SaveFactoryRequest request, List<MultipartFile> images) {
        if (request.version() == null) {
            throw new BadRequestException("Version is required for factory updates");
        }

        Factory factory = factoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Factory not found"));

        factory.setVersion(request.version());

        return saveOrUpdate(factory, request, images);
    }

    @Cacheable(value = "factories", key = "#id")
    @Transactional(readOnly = true)
    public Factory getFactoryById(UUID id) {
        Factory factory = factoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Factory not found"));

        factory.setImageUrls(new HashSet<>(factory.getImageUrls()));
        return factory;
    }

    @Transactional(readOnly = true)
    public List<Factory> getFactoriesByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return factoryRepository.findAllById(ids);
    }

    @Transactional(readOnly = true)
    public Page<Factory> searchFactories(FactorySearchFilter filter, Pageable pageable) {
        Specification<Factory> spec = (root, query, cb) -> cb.isTrue(cb.literal(true));

        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%"));
        }

        if (filter.countryCode() != null && !filter.countryCode().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("countryCode"), filter.countryCode()));
        }
        if (filter.cityCode() != null && !filter.cityCode().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("cityCode"), filter.cityCode()));
        }

        return factoryRepository.findAll(spec, pageable);
    }

    @CacheEvict(value = "factories", key = "#id")
    @Transactional
    public void deleteFactory(UUID id) {
        factoryStatusRepository.deleteFactoryStatusByFactoryId(id);
        factoryRepository.deleteById(id);
    }

    private Factory saveOrUpdate(Factory factory, SaveFactoryRequest request, List<MultipartFile> images) {
        City city = cityRepository.findByCountryCodeAndCode(request.countryCode(), request.cityCode())
                .orElseThrow(() -> new EntityNotFoundException("City not found"));

        factory.setName(request.name());
        factory.setLocation(new GeoLocation(request.location().getLatitude(), request.location().getLongitude()));
        factory.setStreet(request.street());

        factory.setCountryCode(request.countryCode());
        factory.setCityCode(request.cityCode());

        Set<String> mergedImageUrls = new HashSet<>(request.imageUrls() == null ? Set.of() : request.imageUrls());
        mergedImageUrls.addAll(saveImages(images));
        factory.setImageUrls(mergedImageUrls);

        Factory newFactory = factoryRepository.save(factory);
        newFactory.setImageUrls(new HashSet<>(newFactory.getImageUrls()));
        return newFactory;
    }

    private List<String> saveImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        List<String> savedPaths = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }
            validateImage(image);
            savedPaths.add(saveImage(image));
        }
        return savedPaths;
    }

    private void validateImage(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BadRequestException("Images must be JPG, PNG, or WebP files");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new BadRequestException("Each image must not exceed 10 MB");
        }
    }

    private String saveImage(MultipartFile image) {
        String ext = extensionOrDefault(image.getOriginalFilename(), "jpg");
        String path = "factories/" + UUID.randomUUID() + "." + ext;
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
}