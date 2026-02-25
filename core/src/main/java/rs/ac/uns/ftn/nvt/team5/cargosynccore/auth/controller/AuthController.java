package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.dto.AccountDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.dto.LoginRequestDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.dto.TokenResponseDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.service.AuthService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.manager.dto.PasswordChangeRequest;

import static org.springframework.http.MediaType.*;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = {"/auth", ""}, produces = APPLICATION_JSON_VALUE)
class AuthController {

    private final AuthService authService;
    private final ModelMapper modelMapper;

    @PostMapping("/login")
    TokenResponseDto login(
            @RequestBody @Valid LoginRequestDto request
    ) {
        var accessToken = authService.login(request.username(), request.password());
        return new TokenResponseDto(accessToken);
    }

    @GetMapping("/whoami")
    Object whoami(@AuthenticationPrincipal Account account) {
        return modelMapper.map(account, AccountDto.class);
    }

    @GetMapping("/change-password")
    void changePassword(@AuthenticationPrincipal Account account, @RequestBody @Valid PasswordChangeRequest request) {
        authService.changePassword(account.getUsername(), request.oldPassword(), request.newPassword());
    }
}
