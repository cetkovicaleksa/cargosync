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

import java.net.URI;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/vans", produces = APPLICATION_JSON_VALUE)
class VanController {

    private final UpdateVanUC updateVanUC;
    private final NewVanUC newVanUC;
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

    @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    ResponseEntity<VanDto> addAVan(
            @ModelAttribute VanForm form
    ) {
        var van = newVanUC.createVan(form);
        String etag = Integer.toHexString(van.getVersion());

        return ResponseEntity.created(URI.create("/vans/" + van.getVin()))
                .eTag(etag) // ?
                .body(modelMapper.map(van, VanDto.class));
    }

    @PatchMapping(path = "/{vin}", consumes = MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    ResponseEntity<VanDto> updateVan(
            @PathVariable String vin,
            @ModelAttribute UpdateVanForm form
    ) {
        form.setVin(vin);

        var van = updateVanUC.updateVan(form);
        String etag = Integer.toHexString(van.getVersion());

        return ResponseEntity.ok()
                .eTag(etag)
                .body(modelMapper.map(van, VanDto.class));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(NO_CONTENT)
    void deleteAVan(
            @PathVariable String id
    ) {
        deleteVanUc.deleteVan(id);
    }

}
