package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.HttpUtil;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service.*;

import java.net.URI;
import java.time.Instant;

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
        private final VanAnalyticsService vanAnalyticsService;
    private final ModelMapper modelMapper;
    private final HttpUtil httpUtil;

    @GetMapping
    Page<VanDto> getVans(
            @Valid @ModelAttribute VanQueryParams params,
            Pageable pageable
    ) {
        params.setPageable(pageable);

        return queryVansUc.getAllVans(params)
                .map(van -> modelMapper.map(van, VanDto.class));
    }

    @GetMapping("/{vin}")
    ResponseEntity<VanDto> getAVan(
            @PathVariable String vin
    ) {
        var van = queryVansUc.getVan(vin);
        String etag = httpUtil.serializeEtag(van.getVersion());

        return ResponseEntity.ok()
                .eTag(etag)
                // NOTE: Not sure what cache properties
                .body(modelMapper.map(van, VanDto.class));
    }

        @GetMapping("/{vin}/status")
        VanStatusDto getVanStatus(@PathVariable String vin) {
                return vanAnalyticsService.getStatus(vin);
        }

        @GetMapping("/{vin}/distance")
        VanDistanceSeriesDto getVanDistanceSeries(
                        @PathVariable String vin,
                        @RequestParam Instant from,
                        @RequestParam Instant to
        ) {
                return vanAnalyticsService.getDistanceSeries(vin, from, to);
        }

    @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    ResponseEntity<VanDto> addAVan(
            @Valid @ModelAttribute VanForm form
    ) {
        var van = newVanUC.createVan(form);
        String etag = httpUtil.serializeEtag(van.getVersion());

        return ResponseEntity.created(URI.create("/vans/" + van.getVin()))
                .eTag(etag) // ?
                .body(modelMapper.map(van, VanDto.class));
    }

    @PatchMapping(path = "/{vin}", consumes = MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    ResponseEntity<VanDto> updateVan(
            @PathVariable String vin,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @ModelAttribute UpdateVanForm form
    ) {
        form.setVin(vin);
        try {
            int version = httpUtil.parseEtag(ifMatch);
            form.setVersion(version);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(PRECONDITION_FAILED).build();
        }

        var van = updateVanUC.updateVan(form);
        String etag = httpUtil.serializeEtag(van.getVersion());

        return ResponseEntity.ok()
                .eTag(etag)
                .body(modelMapper.map(van, VanDto.class));
    }

    @DeleteMapping("/{vin}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(NO_CONTENT)
    void deleteAVan(
            @PathVariable String vin
    ) {
        deleteVanUc.deleteVan(vin);
    }

}
