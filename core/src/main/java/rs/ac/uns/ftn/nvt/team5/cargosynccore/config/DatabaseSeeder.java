package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import jakarta.annotation.Nonnull;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.repository.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.manager.service.ManagerService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
@Order(1)
public class DatabaseSeeder implements CommandLineRunner {
    private static final String DEFAULT_SUPER_MANAGER_AVATAR_CLASSPATH = "images/default_avatar.png";
    private static final String DEFAULT_SUPER_MANAGER_PASSWORD = "1234";

    private final AccountRepository accounts;
    private final ManagerService managerService;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.supermanager-email}")
    private String superManagerEmail;

    @Transactional
    @Override
    public void run(@Nonnull String... args) throws Exception {

        if (!accounts.existsByUsername("admin")) {
            // Initial super manager
            try {
                managerService.createManger(superManagerEmail, "admin", DEFAULT_SUPER_MANAGER_AVATAR_CLASSPATH, true);
            } catch (Exception e) {
                log.error("Failed to create super manager", e);
            }
        }

        accounts.findByUsername("admin").ifPresent(admin -> {
            admin.setPassword(passwordEncoder.encode(DEFAULT_SUPER_MANAGER_PASSWORD));
            admin.setEnabled(true);
            admin.setAccountNonLocked(true);
            admin.setCredentialsNonExpired(true);
            accounts.save(admin);
        });
    }

}
