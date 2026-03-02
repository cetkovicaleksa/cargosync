package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Customer;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Manager;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CustomerRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.ManagerRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileAccessController {
    private final CustomerRepository customerRepository;
    private final ManagerRepository managerRepository;

    @GetMapping("/check-access")
    public ResponseEntity<Void> checkFileAccess(HttpServletRequest request) {
        String originalUri = request.getHeader("X-Original-URI");
        log.debug("Checking access for file: {}", originalUri);

        if (originalUri == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!originalUri.startsWith("/uploads/")) {
            return ResponseEntity.badRequest().build();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Account account)) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = account.getOwner().getId();
        String image = originalUri.replaceFirst("^/uploads/", "");

        // All authenticated users can see product images
        if (originalUri.startsWith("/uploads/products/")) {
            if (auth.isAuthenticated() && !auth.getName().equals("anonymousUser"))
                return ResponseEntity.ok().build();
        }

        // Managers can see all avatars, customers can only see their own
        if (originalUri.startsWith("/uploads/avatars/")) {
            if (isManager(auth))
                return ResponseEntity.ok().build();
            if (isCustomer(auth)) {
                Optional<Customer> customer = customerRepository.findByAvatar(image);
                if (customer.isPresent() && userId.equals(customer.get().getId()))
                    return ResponseEntity.ok().build();
            }
        }

        if (originalUri.startsWith("/uploads/factories/")) {
            if (isManager(auth))
                return ResponseEntity.ok().build();
        }

        if (originalUri.startsWith("/uploads/warehouses/")) {
            if (isManager(auth))
                return ResponseEntity.ok().build();
        }

        log.debug("Access denied for URI: {} by user: {}", originalUri, auth.getName());
        return ResponseEntity.status(403).build();
    }

    private boolean isManager(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_MANAGER"));
    }

    private boolean isCustomer(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_CUSTOMER"));
    }

    private boolean isSuperManager(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> Objects.equals(a.getAuthority(), "MANAGE_MANAGERS"));
    }
}
