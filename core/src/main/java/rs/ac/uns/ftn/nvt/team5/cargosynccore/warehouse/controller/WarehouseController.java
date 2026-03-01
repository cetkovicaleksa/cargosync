package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.service.*;

import java.net.URI;
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
    private final ModelMapper modelMapper;

    @GetMapping
    Page<WarehouseDto> getWarehouses(
            @ModelAttribute WarehouseQueryParams params,
            Pageable pageable
    ) {
        return queryWarehousesUC.getWarehouses(params, pageable)
                .map(w -> modelMapper.map(w, WarehouseDto.class));
    }

    @GetMapping("/{id}")
    WarehouseDto getWarehouse(@PathVariable UUID id) {
        return modelMapper.map(queryWarehousesUC.getWarehouse(id), WarehouseDto.class);
    }

    @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    ResponseEntity<WarehouseDto> createWarehouse(@ModelAttribute WarehouseForm form) {
        var warehouse = createWarehouseUC.createWarehouse(form);
        return ResponseEntity.created(URI.create("/warehouses/" + warehouse.getId()))
                .body(modelMapper.map(warehouse, WarehouseDto.class));
    }

    @PutMapping(path = "/{id}", consumes = MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    WarehouseDto updateWarehouse(@PathVariable UUID id, @ModelAttribute UpdateWarehouseForm form) {
        return modelMapper.map(updateWarehouseUC.updateWarehouse(id, form), WarehouseDto.class);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(NO_CONTENT)
    void deleteWarehouse(@PathVariable UUID id) {
        deleteWarehouseUC.deleteWarehouse(id);
    }
}
