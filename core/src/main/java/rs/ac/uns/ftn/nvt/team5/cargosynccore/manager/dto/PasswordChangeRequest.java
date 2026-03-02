package rs.ac.uns.ftn.nvt.team5.cargosynccore.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank String username,
        @NotBlank String oldPassword,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String newPassword
) {}
