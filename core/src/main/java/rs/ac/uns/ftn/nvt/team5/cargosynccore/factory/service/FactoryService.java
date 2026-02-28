package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactorySearchFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.SaveFactoryRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.City;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.Factory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.FactoryStatus;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.GeoLocation;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CityRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository.FactoryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository.FactoryStatusRepository;

import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FactoryService {

    private final FactoryRepository factoryRepository;
    private final FactoryStatusRepository factoryStatusRepository;
    private final CityRepository cityRepository;

    @Transactional
    public Factory createFactory(SaveFactoryRequest request) {
        Factory factory = new Factory();
        Factory savedFactory = saveOrUpdate(factory, request);

        FactoryStatus status = new FactoryStatus(savedFactory);
        factoryStatusRepository.save(status);

        return savedFactory;
    }

//    @CachePut(value = "factories", key = "#id")
    @Transactional
    public Factory updateFactory(UUID id, SaveFactoryRequest request) {
        Factory factory = factoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Factory not found"));

        if (request.version() != null) {
            factory.setVersion(request.version());
        }

        return saveOrUpdate(factory, request);
    }

//    @Cacheable(value = "factories", key = "#id")
    @Transactional(readOnly = true)
    public Factory getFactoryById(UUID id) {
        Factory factory = factoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Factory not found"));

        Hibernate.initialize(factory.getImageUrls());
        return factory;
    }

    @Transactional(readOnly = true)
    public Page<Factory> searchFactories(FactorySearchFilter filter, Pageable pageable) {
        Specification<Factory> spec = (root, query, cb) -> cb.isTrue(cb.literal(true));

        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%"));
        }
        if (filter.countryCode() != null && !filter.countryCode().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("city").get("country").get("code"), filter.countryCode()));
        }
        if (filter.cityCode() != null && !filter.cityCode().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("city").get("code"), filter.cityCode()));
        }

        return factoryRepository.findAll(spec, pageable);
    }

//    @CacheEvict(value = "factories", key = "#id")
    @Transactional
    public void deleteFactory(UUID id) {
        factoryRepository.deleteById(id);
    }

    private Factory saveOrUpdate(Factory factory, SaveFactoryRequest request) {
        City city = cityRepository.findByCountryCodeAndCode(request.countryCode(), request.cityCode())
                .orElseThrow(() -> new EntityNotFoundException("City not found"));

        factory.setName(request.name());
        factory.setLocation(new GeoLocation(request.location().getLatitude(), request.location().getLongitude()));
        factory.setStreet(request.street());
        factory.setCity(city);

        if (request.imageUrls() != null) {
            factory.setImageUrls(new HashSet<>(request.imageUrls()));
        }

        return factoryRepository.save(factory);
    }
}