package rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "security.auth.jwt")
public record JwtProperties (
        @NotBlank
        String issuer,

        @Pattern(regexp = "HS256|HS384|HS512")
        String algorithm,

        @Nonnull
        TokenProperties accessToken
) {
    public record TokenProperties(
            @Pattern(regexp = "^(?:[0-9a-fA-F]{2})+$") // hex encoded secret must be even length
            String secret,

            @Positive Duration ttl
    ) { }
}
