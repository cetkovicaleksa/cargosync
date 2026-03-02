package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryAvailabilitySeriesDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryLookupDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryProductionSeriesDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactorySearchFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.SaveFactoryRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.Factory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.service.FactoryAnalyticsService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.service.FactoryService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto.ProductSummaryDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.service.ProductService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.CityOptionDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.CountryOptionDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.GeoLocationDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.City;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CountryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CityRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.CountryService;

import java.util.Comparator;
import java.util.List;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@RestController
@RequestMapping("/factories")
@RequiredArgsConstructor
public class FactoryController {

    private final FactoryService factoryService;
    private final FactoryAnalyticsService factoryAnalyticsService;
    private final CityRepository cityRepository;
    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public FactoryDto createFactory(
            @RequestPart("data") @Valid SaveFactoryRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return mapToDto(factoryService.createFactory(request, images));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    public FactoryDto updateFactory(
            @PathVariable UUID id,
            @RequestPart("data") @Valid SaveFactoryRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return mapToDto(factoryService.updateFactory(id, request, images));
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public Page<FactoryDto> searchFactories(FactorySearchFilter filter, Pageable pageable) {
        return factoryService.searchFactories(filter, pageable).map(this::mapToDto);
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasRole('MANAGER')")
    public Page<FactoryLookupDto> searchFactoriesLookup(FactorySearchFilter filter, Pageable pageable) {
        return factoryService.searchFactories(filter, pageable).map(this::mapToLookupDto);
    }

    @GetMapping("/by-ids")
    @PreAuthorize("hasRole('MANAGER')")
    public List<FactoryDto> getFactoriesByIds(@RequestParam List<UUID> ids) {
        return factoryService.getFactoriesByIds(ids).stream().map(this::mapToDto).toList();
    }

    @GetMapping("/by-ids/lookup")
    @PreAuthorize("hasRole('MANAGER')")
    public List<FactoryLookupDto> getFactoriesByIdsLookup(@RequestParam List<UUID> ids) {
        return factoryService.getFactoriesByIds(ids).stream().map(this::mapToLookupDto).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public FactoryDto getFactoryById(@PathVariable UUID id) {
        return mapToDto(factoryService.getFactoryById(id));
    }

    @GetMapping("/{id}/products")
    @PreAuthorize("hasRole('MANAGER')")
    public List<ProductSummaryDto> getFactoryProducts(@PathVariable UUID id) {
        return productService.getProductsByFactoryId(id);
    }

    @GetMapping("/{id}/products/{productId}/production")
    @PreAuthorize("hasRole('MANAGER')")
    public FactoryProductionSeriesDto getFactoryProductProduction(
            @PathVariable UUID id,
            @PathVariable UUID productId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return factoryAnalyticsService.getProductionSeries(
                id,
                productId,
                parseToInstant(from, false),
                parseToInstant(to, true)
        );
    }

    @GetMapping("/{id}/availability")
    @PreAuthorize("hasRole('MANAGER')")
    public FactoryAvailabilitySeriesDto getFactoryAvailability(
            @PathVariable UUID id,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return factoryAnalyticsService.getAvailabilitySeries(
                id,
                parseToInstant(from, false),
                parseToInstant(to, true)
        );
    }

    private Instant parseToInstant(String value, boolean endOfDayForDateOnly) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("'from' and 'to' query params are required");
        }

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // fallback to date-only values (yyyy-MM-dd)
        }

        try {
            LocalDate date = LocalDate.parse(value);
            if (endOfDayForDateOnly) {
                return date.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
            }
            return date.atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid datetime format for value: " + value);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFactory(@PathVariable UUID id) {
        factoryService.deleteFactory(id);
    }

    private FactoryDto mapToDto(Factory factory) {
        City city = cityRepository.findByCountryCodeAndCode(factory.getCountryCode(), factory.getCityCode())
                .orElseThrow(() -> new IllegalStateException("City missing"));
        GeoLocationDto geoDto = new GeoLocationDto();
        geoDto.setLatitude(factory.getLocation().getLatitude());
        geoDto.setLongitude(factory.getLocation().getLongitude());

        return new FactoryDto(
                factory.getId(),
                factory.getName(),
                geoDto,
                factory.getStreet(),
                city.getCode(),
                city.getName(),
                city.getCountry().getCode(),
                city.getCountry().getName(),
                factory.getImageUrls(),
                factory.getVersion()
        );
    }


    private FactoryLookupDto mapToLookupDto(Factory factory) {
        City city = cityRepository.findByCountryCodeAndCode(factory.getCountryCode(), factory.getCityCode())
                .orElseThrow(() -> new IllegalStateException("City missing"));

        return new FactoryLookupDto(
                factory.getId(),
                factory.getName(),
                factory.getStreet(),
                city.getName(),
                city.getCountry().getName(),
                city.getCountry().getCode()
        );
    }
}