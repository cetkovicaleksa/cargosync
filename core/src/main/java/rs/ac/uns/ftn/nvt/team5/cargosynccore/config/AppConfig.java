package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan(
        basePackages = {"rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties"}
)
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
