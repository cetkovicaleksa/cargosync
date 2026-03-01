package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import jakarta.annotation.Nonnull;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
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
public class DatabaseSeeder implements CommandLineRunner {
    private static final String DEFAULT_SUPER_MANAGER_AVATAR_CLASSPATH = "images/default_avatar.png";

    private final AccountRepository accounts;
    private final ManagerService managerService;
    @Value("${app.supermanager-email}")
    private String superManagerEmail;
    @Value("${app.supermanager-username}")
    private String superManagerUsername;

    @Transactional
    @Override
    public void run(@Nonnull String... args) throws Exception {

        if (!accounts.existsByUsername("admin")) {
            // Initial super manager
            try {
                managerService.createManger(superManagerEmail, superManagerUsername, DEFAULT_SUPER_MANAGER_AVATAR_CLASSPATH);
            } catch (Exception e) {
                log.error("Failed to create super manager", e);
            }
        }
    }

}
