package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactorySearchFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.SaveFactoryRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.Factory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.service.FactoryService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.GeoLocationDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.City;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CityRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/factories")
@RequiredArgsConstructor
public class FactoryController {

    private final FactoryService factoryService;
    private final CityRepository cityRepository;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public FactoryDto createFactory(@RequestBody @Valid SaveFactoryRequest request) {
        return mapToDto(factoryService.createFactory(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public FactoryDto updateFactory(@PathVariable UUID id, @RequestBody @Valid SaveFactoryRequest request) {
        return mapToDto(factoryService.updateFactory(id, request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<FactoryDto> searchFactories(FactorySearchFilter filter, Pageable pageable) {
        return factoryService.searchFactories(filter, pageable).map(this::mapToDto);
    }

    @GetMapping("/by-ids")
    @PreAuthorize("isAuthenticated()")
    public List<FactoryDto> getFactoriesByIds(@RequestParam List<UUID> ids) {
        return factoryService.getFactoriesByIds(ids).stream().map(this::mapToDto).toList();
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
}