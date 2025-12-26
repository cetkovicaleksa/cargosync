package rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "security")
public record SecurityProperties (
    @DefaultValue String[] publicEndpoints,
    CorsProperties cors
) {}
