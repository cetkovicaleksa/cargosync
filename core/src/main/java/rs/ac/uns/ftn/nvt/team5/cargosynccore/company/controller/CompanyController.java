package rs.ac.uns.ftn.nvt.team5.cargosynccore.company.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.repository.CompanyRepository;

import java.util.List;
import java.util.UUID;

@Tag(name = "Companies")
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyRepository companyRepository;

    public record CompanyOptionDto(UUID id, String name, String address) {}

    @Operation(summary = "My companies", description = "Returns all companies owned by the authenticated customer. Used for order destination selection.")
    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<CompanyOptionDto> getMyCompanies(@AuthenticationPrincipal Account account) {
        return companyRepository.findByOwner_Id(account.getOwner().getId()).stream()
                .map(c -> new CompanyOptionDto(c.getId(), c.getName(), c.getAddress()))
                .toList();
    }
}
