package rs.ac.uns.ftn.nvt.team5.cargosynccore.customer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.AccountRole;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.ActivationToken;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.repository.AccountRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.repository.ActivationTokenRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties.AppProperties;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.customer.dto.RegisterCustomerRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.BadRequestException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.ConflictException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Customer;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.ContentStore;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.EmailService;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CustomerService {

    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_AVATAR_SIZE = 10 * 1024 * 1024; // 10 MB

    private final AccountRepository accountRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final ContentStore contentStore;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Transactional
    public void register(RegisterCustomerRequest request, MultipartFile avatar) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        if (avatar != null && !avatar.isEmpty()) {
            validateAvatar(avatar);
        }

        if (accountRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("An account with this email already exists");
        }

        String avatarPath = (avatar != null && !avatar.isEmpty()) ? saveAvatar(avatar) : null;

        var customer = new Customer(
                request.email(),
                avatarPath,
                request.firstName(),
                request.lastName()
        );

        var account = new Account(
                request.email(),
                passwordEncoder.encode(request.password()),
                AccountRole.CUSTOMER,
                customer,
                false,
                true,
                true
        );

        account = accountRepository.save(account);

        var token = new ActivationToken(
                account,
                LocalDateTime.now().plus(appProperties.activationTokenTtl())
        );
        token = activationTokenRepository.save(token);

        emailService.sendActivationEmail(
                request.email(),
                request.firstName(),
                token.getId().toString()
        );
    }

    @Transactional
    public void activate(UUID tokenId) {
        var token = activationTokenRepository.findById(tokenId)
                .orElseThrow(() -> new NotFoundException("Activation token not found"));

        if (token.isUsed()) {
            throw new BadRequestException("Activation token has already been used");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Activation token has expired");
        }

        var account = token.getAccount();
        account.setEnabled(true);
        token.setUsed(true);

        accountRepository.save(account);
        activationTokenRepository.save(token);
    }

    private void validateAvatar(MultipartFile avatar) {
        String contentType = avatar.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new BadRequestException("Avatar must be a JPG, PNG, or WebP image");
        }
        if (avatar.getSize() > MAX_AVATAR_SIZE) {
            throw new BadRequestException("Avatar image must not exceed 10 MB");
        }
    }

    private String saveAvatar(MultipartFile avatar) {
        String original = avatar.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "jpg";
        String path = "avatars/customers/" + UUID.randomUUID() + "." + ext;
        contentStore.save(path, avatar);
        return path;
    }

}
