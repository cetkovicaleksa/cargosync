package rs.ac.uns.ftn.nvt.team5.cargosynccore.manager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateManagerRequest(
    @NotBlank @Email String email,
    @NotBlank String username
) {}
