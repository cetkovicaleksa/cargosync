package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryMapper;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactorySearchFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.SaveFactoryRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.service.FactoryService;

import java.util.UUID;

@RestController
@RequestMapping("/factories")
@RequiredArgsConstructor
public class FactoryController {

    private final FactoryService factoryService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public FactoryDto createFactory(@RequestBody @Valid SaveFactoryRequest request) {
        return FactoryMapper.mapToDto(factoryService.createFactory(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public FactoryDto updateFactory(@PathVariable UUID id, @RequestBody @Valid SaveFactoryRequest request) {
        return FactoryMapper.mapToDto(factoryService.updateFactory(id, request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<FactoryDto> searchFactories(FactorySearchFilter filter, Pageable pageable) {
        return factoryService.searchFactories(filter, pageable).map(FactoryMapper::mapToDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFactory(@PathVariable UUID id) {
        factoryService.deleteFactory(id);
    }
}