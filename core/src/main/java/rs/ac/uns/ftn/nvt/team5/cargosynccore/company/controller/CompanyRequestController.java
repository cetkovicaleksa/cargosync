package rs.ac.uns.ftn.nvt.team5.cargosynccore.company.controller;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.dto.CompanyRequestDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.service.CompanyService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.ReasonDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.model.CompanyRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.HttpUtil;

import java.util.UUID;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/company-requests", produces = APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('MANAGER')")
class CompanyRequestController {

    private final CompanyService companyService;
    private final ModelMapper modelMapper;
    private final HttpUtil httpUtil;

    @GetMapping
    Object getAllCompanyRequests(
            @RequestParam(required = false, defaultValue = "PENDING") CompanyRequest.Status status,
            Pageable pageable
    ) {
        return companyService.getAllCompanyRequests(status, pageable)
                .map(request -> modelMapper.map(request, CompanyRequestDto.class));
    }

    @GetMapping("/{id}")
    ResponseEntity<CompanyRequestDto> getACompanyRequest(
            @PathVariable UUID id
    ) {
        var request = companyService.getCompanyRequest(id);
        String etag = httpUtil.serializeEtag(request.getVersion());

        return ResponseEntity.ok()
                .eTag(etag)
                .body(modelMapper.map(request, CompanyRequestDto.class));
    }

    @PostMapping("/{id}/approve")
    ResponseEntity<Void> approveRequest(
            @PathVariable UUID id,
            @RequestHeader("If-Match") String ifMatch
    ) {
        try {
            int version = httpUtil.parseEtag(ifMatch);

            if (version != companyService.getCompanyRequest(id).getVersion()) // not fully correct
                return ResponseEntity.status(CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(PRECONDITION_FAILED).build();
        }

        companyService.approveCompanyRequest(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    ResponseEntity<Void> rejectRequest(
            @PathVariable UUID id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestBody ReasonDto body
    ) {
        try {
            int version = httpUtil.parseEtag(ifMatch);

            if (version != companyService.getCompanyRequest(id).getVersion())
                return ResponseEntity.status(CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(PRECONDITION_FAILED).build();
        }

        companyService.rejectCompanyRequest(id, body.getReason());
        return ResponseEntity.noContent().build();
    }

}
