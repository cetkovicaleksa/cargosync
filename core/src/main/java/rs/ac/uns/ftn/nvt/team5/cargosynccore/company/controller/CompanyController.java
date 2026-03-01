package rs.ac.uns.ftn.nvt.team5.cargosynccore.company.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.dto.CompanyRequestForm;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.service.CompanyService;

import static org.springframework.http.MediaType.*;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/companies", produces = APPLICATION_JSON_VALUE)
class CompanyController {
    private final CompanyService companyService;

    @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    ResponseEntity<Void> requestACompany(
            @Valid @ModelAttribute CompanyRequestForm form
    ) {
        companyService.requestCompany(form);
        return ResponseEntity.accepted()
                .build();
    }

}
