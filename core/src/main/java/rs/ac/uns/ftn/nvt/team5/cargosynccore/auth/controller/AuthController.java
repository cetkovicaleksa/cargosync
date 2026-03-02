package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.dto.AccountDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.dto.LoginRequestDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.dto.TokenResponseDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.service.AuthService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.service.TokenBlacklistService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.manager.dto.PasswordChangeRequest;

import static org.springframework.http.MediaType.*;

@Tag(name = "Auth")
@RequiredArgsConstructor
@RestController
@RequestMapping(value = {"/auth", ""}, produces = APPLICATION_JSON_VALUE)
class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklist;
    private final ModelMapper modelMapper;

    @Operation(summary = "Login", description = "Authenticate with username and password, receive a JWT access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Account not active")
    })
    @PostMapping("/login")
    TokenResponseDto login(
            @RequestBody @Valid LoginRequestDto request
    ) {
        var accessToken = authService.login(request.username(), request.password());
        return new TokenResponseDto(accessToken);
    }

    @Operation(summary = "Whoami", description = "Returns the currently authenticated account.")
    @GetMapping("/whoami")
    Object whoami(@AuthenticationPrincipal Account account) {
        return modelMapper.map(account, AccountDto.class);
    }

    @Operation(summary = "Change password", description = "Changes the password and returns a new JWT token.")
    @PostMapping("/change-password")
    TokenResponseDto changePassword(@RequestBody @Valid PasswordChangeRequest request) {
        var accessToken = authService.changePassword(request.username(), request.oldPassword(), request.newPassword());
        return new TokenResponseDto(accessToken);
    }

    @Operation(summary = "Logout — blacklists the JWT token in Redis")
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenBlacklist.blacklist(authHeader.substring("Bearer ".length()));
        }
        return ResponseEntity.ok().build();
    }
}
