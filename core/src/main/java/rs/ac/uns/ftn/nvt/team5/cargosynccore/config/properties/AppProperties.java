package rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("http://localhost:4200") String frontendUrl,
        @DefaultValue("24h") Duration activationTokenTtl
) {}
