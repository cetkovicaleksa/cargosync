package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.*;
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

    @Operation(summary = "Create factory", description = "Creates a new factory with optional images. Requires MANAGER role.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public FactoryDto createFactory(
            @RequestPart("data") @Valid SaveFactoryRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return factoryService.createFactory(request, images);
    }

    @Operation(summary = "Update factory", description = "Updates an existing factory with optional images. Requires MANAGER role.")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    public FactoryDto updateFactory(
            @PathVariable UUID id,
            @RequestPart("data") @Valid SaveFactoryRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return factoryService.updateFactory(id, request, images);
    }

    @Operation(summary = "Search factories", description = "Searches for factories based on filter criteria. Requires MANAGER role.")
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public Page<FactoryDto> searchFactories(FactorySearchFilter filter, Pageable pageable) {
        return factoryService.searchFactories(filter, pageable).map(f -> FactoryMapper.mapToDto(f, cityRepository));
    }

    @Operation(summary = "Search factories (lookup)", description = "Searches for factories based on filter criteria. Requires MANAGER role.")
    @GetMapping("/lookup")
    @PreAuthorize("hasRole('MANAGER')")
    public Page<FactoryLookupDto> searchFactoriesLookup(FactorySearchFilter filter, Pageable pageable) {
        return factoryService.searchFactories(filter, pageable).map(f -> FactoryMapper.mapToLookupDto(f, cityRepository));
    }

    @Operation(summary = "Get factories by ids", description = "Retrieves a list of factories by their IDs. Requires MANAGER role.")
    @GetMapping("/by-ids")
    @PreAuthorize("hasRole('MANAGER')")
    public List<FactoryDto> getFactoriesByIds(@RequestParam List<UUID> ids) {
        return factoryService.getFactoriesByIds(ids).stream().map(f -> FactoryMapper.mapToDto(f, cityRepository)).toList();
    }

    @Operation(summary = "Get factories by ids (lookup)", description = "Retrieves a list of factories by their IDs. Requires MANAGER role.")
    @GetMapping("/by-ids/lookup")
    @PreAuthorize("hasRole('MANAGER')")
    public List<FactoryLookupDto> getFactoriesByIdsLookup(@RequestParam List<UUID> ids) {
        return factoryService.getFactoriesByIds(ids).stream().map(f -> FactoryMapper.mapToLookupDto(f, cityRepository)).toList();
    }

    @Operation(summary = "Get factory by id", description = "Retrieves a factory by its ID. Requires MANAGER role.")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public FactoryDto getFactoryById(@PathVariable UUID id) {
        return factoryService.getFactoryById(id);
    }

    @Operation(summary = "Get factory products", description = "Retrieves a list of products for a factory. Requires MANAGER role.")
    @GetMapping("/{id}/products")
    @PreAuthorize("hasRole('MANAGER')")
    public List<ProductSummaryDto> getFactoryProducts(@PathVariable UUID id) {
        return productService.getProductsByFactoryId(id);
    }

    @Operation(summary = "Get factory product production", description = "Retrieves production information for a factory product. Requires MANAGER role.")
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

    @Operation(summary = "Get factory product inventory", description = "Retrieves inventory information for a factory product. Requires MANAGER role.")
    @GetMapping("/{id}/production/{productId}/inventory")
    @PreAuthorize("hasRole('MANAGER')")
    public FactoryInventoryDto getFactoryProductInventory(
            @PathVariable UUID id,
            @PathVariable UUID productId
    ) {
        return factoryService.getFactoryProductInventory(id, productId);
    }

    @Operation(summary = "Get factory availability", description = "Retrieves availability information for a factory. Requires MANAGER role.")
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

    @Operation(summary = "Get factory status", description = "Retrieves current status of a factory. Requires MANAGER role.")
    @GetMapping("/{id}/status")
    @PreAuthorize("hasRole('MANAGER')")
    public FactoryStatusDto getFactoryStatus(@PathVariable UUID id) {
        return factoryService.getFactoryStatus(id);
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
}