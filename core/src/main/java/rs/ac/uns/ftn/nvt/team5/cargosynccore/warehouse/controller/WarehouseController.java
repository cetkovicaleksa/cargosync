package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CityRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CountryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.service.*;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.*;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/warehouses", produces = APPLICATION_JSON_VALUE)
public class WarehouseController {

    private final QueryWarehousesUC queryWarehousesUC;
    private final CreateWarehouseUC createWarehouseUC;
    private final UpdateWarehouseUC updateWarehouseUC;
    private final DeleteWarehouseUC deleteWarehouseUC;
    private final TemperatureChartService temperatureChartService;
    private final ModelMapper modelMapper;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;

    public record CountryOptionDto(String code, String name) {}
    public record CityOptionDto(String code, String name) {}

    @GetMapping
    Page<WarehouseDto> getWarehouses(
            @ModelAttribute WarehouseQueryParams params,
            Pageable pageable
    ) {
        return queryWarehousesUC.getWarehouses(params, pageable).toPage(pageable);
    }

    @GetMapping("/{id}")
    WarehouseDto getWarehouse(@PathVariable UUID id) {
        return queryWarehousesUC.getWarehouse(id);
    }

    @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    @Caching(evict = {
            @CacheEvict(cacheNames = "warehouseList", allEntries = true),
            @CacheEvict(cacheNames = "warehouseDetail", allEntries = true)
    })
    ResponseEntity<WarehouseDto> createWarehouse(@ModelAttribute WarehouseForm form) {
        var warehouse = createWarehouseUC.createWarehouse(form);
        return ResponseEntity.created(URI.create("/warehouses/" + warehouse.getId()))
                .body(modelMapper.map(warehouse, WarehouseDto.class));
    }

    @PutMapping(path = "/{id}", consumes = MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    @Caching(evict = {
            @CacheEvict(cacheNames = "warehouseList", allEntries = true),
            @CacheEvict(cacheNames = "warehouseDetail", key = "#id")
    })
    WarehouseDto updateWarehouse(@PathVariable UUID id, @ModelAttribute UpdateWarehouseForm form) {
        return modelMapper.map(updateWarehouseUC.updateWarehouse(id, form), WarehouseDto.class);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(NO_CONTENT)
    @Caching(evict = {
            @CacheEvict(cacheNames = "warehouseList", allEntries = true),
            @CacheEvict(cacheNames = "warehouseDetail", key = "#id")
    })
    void deleteWarehouse(@PathVariable UUID id) {
        deleteWarehouseUC.deleteWarehouse(id);
    }

    @GetMapping("/countries")
    @PreAuthorize("isAuthenticated()")
    List<CountryOptionDto> getAllCountries() {
        return countryRepository.findAll().stream()
                .sorted(Comparator.comparing(c -> c.getName().toLowerCase()))
                .map(c -> new CountryOptionDto(c.getCode(), c.getName()))
                .toList();
    }

    @GetMapping("/cities")
    @PreAuthorize("isAuthenticated()")
    List<CityOptionDto> getCitiesByCountry(@RequestParam String countryCode) {
        return cityRepository.findByCountryCodeOrderByNameAsc(countryCode).stream()
                .map(c -> new CityOptionDto(c.getCode(), c.getName()))
                .toList();
    }

    @GetMapping("/{id}/sectors/{sectorId}/temperature-chart")
    @PreAuthorize("hasRole('MANAGER')")
    TemperatureChartDto getTemperatureChart(
            @PathVariable UUID id,
            @PathVariable UUID sectorId,
            @RequestParam(defaultValue = "LAST_7D") TemperatureChartPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return temperatureChartService.getChart(id, sectorId, period, from, to);
    }
}
