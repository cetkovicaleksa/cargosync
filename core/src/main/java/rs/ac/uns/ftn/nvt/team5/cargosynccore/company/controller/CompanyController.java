package rs.ac.uns.ftn.nvt.team5.cargosynccore.company.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.dto.CompanyDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.dto.CompanyRequestForm;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.service.CompanyService;

import static org.springframework.http.MediaType.*;

@Tag(name = "Companies")
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/companies", produces = APPLICATION_JSON_VALUE)
class CompanyController {
    private final CompanyService companyService;
    private final ModelMapper modelMapper;

    @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    ResponseEntity<Void> requestACompany(
            @Valid @ModelAttribute CompanyRequestForm form
    ) {
        companyService.requestCompany(form);
        return ResponseEntity.accepted()
                .build();
    }

    @Operation(summary = "My companies", description = "Returns all companies owned by the authenticated customer. Used for order destination selection.")
    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    Object getMyCompanies(
            @AuthenticationPrincipal Account account
    ) {
        return companyService.getCustomerCompanies(account.getOwner().getId()).stream()
                .map(c -> modelMapper.map(c, CompanyDto.class))
                .toList();
    }

}
