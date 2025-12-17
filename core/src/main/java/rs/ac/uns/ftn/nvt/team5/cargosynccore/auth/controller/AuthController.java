package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.dto.LoginRequestDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.dto.TokenResponseDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.service.AuthService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    TokenResponseDto login(
            @RequestBody @Valid LoginRequestDto request
    ) {
        var accessToken = authService.login(request.username(), request.password());
        return new TokenResponseDto(accessToken);
    }

    @GetMapping("/whoami")
    Object whoami(@AuthenticationPrincipal Account account) {
        return account;
    }

}
