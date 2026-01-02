package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service.*;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/vans", produces = APPLICATION_JSON_VALUE)
class VanController {

    private final QueryVansUC queryVansUc;
    private final DeleteVanUC deleteVanUc;
    private final ModelMapper modelMapper;

    @GetMapping
    Page<VanDto> getVans(
            @Valid @ModelAttribute VanQueryParams params,
            Pageable pageable
    ) {
        params.setPageable(pageable);

        return queryVansUc.getAllVans(params)
                .map(van -> modelMapper.map(van, VanDto.class));
    }

    @GetMapping("/{id}")
    ResponseEntity<VanDto> getAVan(
            @PathVariable String id
    ) {
        var van = queryVansUc.getVan(id);
        String etag = Integer.toHexString(van.getVersion());

        return ResponseEntity.ok()
                .eTag(etag)
                // NOTE: Not sure what cache properties
                .body(modelMapper.map(van, VanDto.class));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(NOT_IMPLEMENTED)
    VanDto addAVan() {return null;}

    // TODO: Should we use PUT OR PATCH (maybe both)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(NO_CONTENT)
    void deleteAVan(
            @PathVariable String id
    ) {
        deleteVanUc.deleteVan(id);
    }

}
