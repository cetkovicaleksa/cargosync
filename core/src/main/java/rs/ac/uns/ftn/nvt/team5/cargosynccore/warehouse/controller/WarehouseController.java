package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Warehouses")
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

    @Operation(summary = "List warehouses", description = "Paginated, filterable list of all warehouses. Filter by name, country, city or online status.")
    @GetMapping
    Page<WarehouseDto> getWarehouses(
            @ModelAttribute WarehouseQueryParams params,
            Pageable pageable
    ) {
        return queryWarehousesUC.getWarehouses(params, pageable).toPage(pageable);
    }

    @Operation(summary = "Get warehouse", description = "Returns a single warehouse with its sectors and current status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Warehouse found"),
            @ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping("/{id}")
    WarehouseDto getWarehouse(@PathVariable UUID id) {
        return queryWarehousesUC.getWarehouse(id);
    }

    @Operation(summary = "Create warehouse", description = "Creates a new warehouse with sectors and optional images. Requires MANAGER role.")
    @ApiResponse(responseCode = "201", description = "Warehouse created")
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

    @Operation(summary = "Update warehouse", description = "Updates warehouse fields, sectors and images. Requires MANAGER role.")
    @PutMapping(path = "/{id}", consumes = MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    @Caching(evict = {
            @CacheEvict(cacheNames = "warehouseList", allEntries = true),
            @CacheEvict(cacheNames = "warehouseDetail", key = "#id")
    })
    WarehouseDto updateWarehouse(@PathVariable UUID id, @ModelAttribute UpdateWarehouseForm form) {
        return modelMapper.map(updateWarehouseUC.updateWarehouse(id, form), WarehouseDto.class);
    }

    @Operation(summary = "Delete warehouse", description = "Hard-deletes a warehouse. Fails if the warehouse still has stock inventory. Requires MANAGER role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "409", description = "Warehouse still has stock")
    })
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

    @Operation(summary = "List countries", description = "Returns all available countries for warehouse/company city selection.")
    @GetMapping("/countries")
    @PreAuthorize("isAuthenticated()")
    List<CountryOptionDto> getAllCountries() {
        return countryRepository.findAll().stream()
                .sorted(Comparator.comparing(c -> c.getName().toLowerCase()))
                .map(c -> new CountryOptionDto(c.getCode(), c.getName()))
                .toList();
    }

    @Operation(summary = "List cities by country", description = "Returns cities filtered by country code (e.g. RS, BA).")
    @GetMapping("/cities")
    @PreAuthorize("isAuthenticated()")
    List<CityOptionDto> getCitiesByCountry(
            @Parameter(description = "ISO country code, e.g. RS") @RequestParam String countryCode) {
        return cityRepository.findByCountryCodeOrderByNameAsc(countryCode).stream()
                .map(c -> new CityOptionDto(c.getCode(), c.getName()))
                .toList();
    }

    @Operation(
            summary = "Temperature chart",
            description = "Returns aggregated temperature readings for a warehouse sector. " +
                    "Preset periods: LAST_7D, LAST_30D, LAST_3M, LAST_6M, LAST_1Y. " +
                    "Use CUSTOM with `from` and `to` (ISO-8601) for a custom range (max 1 year). " +
                    "Results are cached in Redis for 5 minutes. Requires MANAGER role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chart data returned"),
            @ApiResponse(responseCode = "400", description = "Invalid period or date range"),
            @ApiResponse(responseCode = "404", description = "Warehouse or sector not found")
    })
    @GetMapping("/{id}/sectors/{sectorId}/temperature-chart")
    @PreAuthorize("hasRole('MANAGER')")
    TemperatureChartDto getTemperatureChart(
            @PathVariable UUID id,
            @PathVariable UUID sectorId,
            @Parameter(description = "Preset period or CUSTOM") @RequestParam(defaultValue = "LAST_7D") TemperatureChartPeriod period,
            @Parameter(description = "Range start (ISO-8601), required when period=CUSTOM") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Range end (ISO-8601), required when period=CUSTOM")  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return temperatureChartService.getChart(id, sectorId, period, from, to);
    }
}
