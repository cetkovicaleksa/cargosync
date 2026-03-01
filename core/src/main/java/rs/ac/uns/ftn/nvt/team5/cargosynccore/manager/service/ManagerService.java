package rs.ac.uns.ftn.nvt.team5.cargosynccore.manager.service;

import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.AccountRole;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.repository.AccountRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.manager.dto.CreateManagerRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Manager;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.User;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.manager-password-file}")
    private String managerPasswordFile;

    @Transactional
    public Account createManager(CreateManagerRequest request) {
        return createManger(request.email(), request.username(), request.avatarUrl());
    }

    @Transactional
    public Account createManger(String email, String username, String avatarUrl) {
        if (accountRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        String avatar = Optional.ofNullable(avatarUrl)
                .orElse("https://ui-avatars.com/api/?name=" + email);

        Manager manager = new Manager(email, avatar);

        String rawPassword = generateRandomPassword();

        logPasswordToFile(email, username, rawPassword);

        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setRole(AccountRole.MANAGER);
        account.setOwner(manager);
        account.setEnabled(true);
        account.setAccountNonLocked(true);
        account.setCredentialsNonExpired(false);

        return accountRepository.save(account);
    }

    @Transactional
    public void toggleBlockManager(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found"));

        if (account.isSuperManager()) {
            throw new SecurityException("Cannot block the Supermanager");
        }

        account.setAccountNonLocked(!account.isAccountNonLocked());
        accountRepository.save(account);
    }

    public Page<Account> searchManagers(String query, Pageable pageable) {
        Specification<Account> spec = (root, q, cb) -> {
            var rolePredicate = cb.equal(root.get("role"), AccountRole.MANAGER);

            if (query == null || query.isBlank()) {
                return rolePredicate;
            }

            String likePattern = "%" + query.toLowerCase() + "%";
            var usernamePredicate = cb.like(cb.lower(root.get("username")), likePattern);
            Join<Account, User> userJoin = root.join("owner");
            var emailPredicate = cb.like(cb.lower(userJoin.get("email")), likePattern);

            return cb.and(rolePredicate, cb.or(usernamePredicate, emailPredicate));
        };

        return accountRepository.findAll(spec, pageable);
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[10];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private synchronized void logPasswordToFile(String email, String username, String password) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(managerPasswordFile, true))) {
            writer.write(String.format("Time: %s | Email: %s | Username: %s | Pass: %s%n",
                    java.time.LocalDateTime.now(), email, username, password));
        } catch (IOException e) {
            log.error("Failed to write password to file", e);
        }
    }
}