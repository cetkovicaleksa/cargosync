package rs.ac.uns.ftn.nvt.team5.cargosynccore.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.customer.dto.RegisterCustomerRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.customer.service.CustomerService;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Customer Registration")
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Register a new customer account")
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(
            @RequestPart("data") @Valid RegisterCustomerRequest request,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar
    ) {
        customerService.register(request, avatar);
        return Map.of("message", "Check your email to activate your account");
    }

    @Operation(summary = "Activate a customer account via email token")
    @GetMapping("/activate")
    public Map<String, String> activate(@RequestParam("token") UUID token) {
        customerService.activate(token);
        return Map.of("message", "Account activated successfully");
    }

}
