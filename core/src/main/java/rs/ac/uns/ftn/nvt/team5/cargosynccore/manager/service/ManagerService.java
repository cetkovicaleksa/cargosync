package rs.ac.uns.ftn.nvt.team5.cargosynccore.manager.service;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.AccountRole;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.repository.AccountRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.manager.dto.CreateManagerRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.BadRequestException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Manager;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.User;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.ContentStore;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerService {

    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_AVATAR_SIZE = 10 * 1024 * 1024; // 10 MB

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContentStore contentStore;
    @Value("${app.manager-password-file}")
    private String managerPasswordFile;

    @Transactional
    public String createManager(CreateManagerRequest request, MultipartFile avatar) {
        return createManger(request.email(), request.username(), avatar);
    }

    @Transactional
    public String createManger(String email, String username, MultipartFile avatarFile) {
        if (accountRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (accountRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        validateAvatar(avatarFile);
        String avatar = saveAvatar(avatarFile);

        return createManagerAccount(email, username, avatar, false);
    }

    @Transactional
    public String createManger(String email, String username, String avatarClasspathPath, boolean superManager) {
        if (accountRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        String avatar = saveAvatarFromClasspath(avatarClasspathPath);

        return createManagerAccount(email, username, avatar, superManager);
    }

    private String createManagerAccount(String email, String username, String avatar, boolean superManager) {

        Manager manager = new Manager(email, avatar);

        String rawPassword = "1234";// generateRandomPassword();

        if (superManager)
            logPasswordToFile(email, username, rawPassword);

        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setRole(AccountRole.MANAGER);
        account.setOwner(manager);
        account.setEnabled(true);
        account.setAccountNonLocked(true);
        account.setCredentialsNonExpired(false);

        accountRepository.save(account);
        if (superManager)
            return "";
        else
            return rawPassword;
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

            String lowerQuery = query.toLowerCase();
            String likePattern = "%" + lowerQuery + "%";

            Join<Account, User> userJoin = root.join("owner");
            
            var usernameLower = cb.lower(root.get("username"));
            var emailLower = cb.lower(userJoin.get("email"));

            var usernamePredicate = cb.like(usernameLower, likePattern);
            var emailPredicate = cb.like(emailLower, likePattern);

            Predicate filter = cb.and(rolePredicate, cb.or(usernamePredicate, emailPredicate));
            
            Expression<Object> priority = cb.selectCase()
                .when(cb.equal(usernameLower, lowerQuery), 0)
                .when(cb.equal(emailLower, lowerQuery), 0)
                .when(cb.like(usernameLower, lowerQuery + "%"), 1)
                .when(cb.like(emailLower, lowerQuery + "%"), 1)
                .otherwise(2);
            q.orderBy(cb.asc(priority), cb.asc(usernameLower));

            return filter;
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

    private void validateAvatar(MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new BadRequestException("Avatar image is required");
        }
        String contentType = avatar.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new BadRequestException("Avatar must be a JPG, PNG, or WebP image");
        }
        if (avatar.getSize() > MAX_AVATAR_SIZE) {
            throw new BadRequestException("Avatar image must not exceed 10 MB");
        }
    }

    private String saveAvatar(MultipartFile avatar) {
        String ext = extensionOrDefault(avatar.getOriginalFilename(), "jpg");
        String path = "avatars/managers/" + UUID.randomUUID() + "." + ext;
        contentStore.save(path, avatar);
        return path;
    }

    private String saveAvatarFromClasspath(String avatarClasspathPath) {
        if (avatarClasspathPath == null || avatarClasspathPath.isBlank()) {
            throw new BadRequestException("Default avatar path is required");
        }

        ClassPathResource resource = new ClassPathResource(avatarClasspathPath);
        if (!resource.exists()) {
            throw new BadRequestException("Default avatar resource not found: " + avatarClasspathPath);
        }

        String ext = extensionOrDefault(resource.getFilename(), "png");
        String path = "avatars/managers/" + UUID.randomUUID() + "." + ext;
        contentStore.save(path, resource);
        return path;
    }

    private String extensionOrDefault(String originalFilename, String fallback) {
        if (originalFilename == null) {
            return fallback;
        }

        int index = originalFilename.lastIndexOf('.');
        if (index < 0 || index == originalFilename.length() - 1) {
            return fallback;
        }

        return originalFilename.substring(index + 1).toLowerCase();
    }
}