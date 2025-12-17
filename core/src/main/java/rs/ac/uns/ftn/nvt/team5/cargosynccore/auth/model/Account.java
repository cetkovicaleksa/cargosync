package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.User;

import java.util.*;

@Getter
@Setter
@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_accounts_username", columnList = "username"),
                @Index(name = "idx_accounts_role", columnList = "role"),
        }
)
public class Account implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @NotBlank
    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, updatable = false)
    private AccountRole role;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true/*, fetch = FetchType.LAZY*/)
    @JoinColumn(name = "owner_id", updatable = false)
    private User owner;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false; // ex. verified email

    @Column(name = "credentials_non_expired", nullable = false)
    private boolean credentialsNonExpired = false; // ex. !requires password reset

    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true; // ex. !banned

    @Override
    @Transient
    public @Nonnull Collection<? extends GrantedAuthority> getAuthorities() {
        // TODO: extract super manager username(s) to properties (and/or SM specific authorities)
        return "admin".equalsIgnoreCase(username)
                ? List.of(role, new SimpleGrantedAuthority("MANAGE_MANAGERS"))
                : List.of(role);
    }

}