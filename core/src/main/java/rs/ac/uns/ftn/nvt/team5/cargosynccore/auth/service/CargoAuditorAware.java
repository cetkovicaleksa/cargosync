package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.service;

import jakarta.annotation.Nonnull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("authAuditorAware")
public class CargoAuditorAware implements AuditorAware<String> {

    private static final String SYSTEM_USER = "system";

    @Override
    @Nonnull
    public Optional<String> getCurrentAuditor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        return Optional.ofNullable(auth)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .or(() -> Optional.of(SYSTEM_USER));
    }

}
