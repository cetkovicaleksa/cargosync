package rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;


@ConfigurationProperties(prefix = "security.cors")
public record CorsProperties (
    @DefaultValue List<String> allowedOrigins,
    @DefaultValue List<String> allowedHeaders,
    @DefaultValue List<String> allowedMethods
) {}
