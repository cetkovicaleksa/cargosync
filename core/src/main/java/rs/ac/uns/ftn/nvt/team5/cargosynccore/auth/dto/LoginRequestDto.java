package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank(message = "{login.email.blank}") String username,
        @NotBlank(message = "{login.password.blank}") String password
) {
}
