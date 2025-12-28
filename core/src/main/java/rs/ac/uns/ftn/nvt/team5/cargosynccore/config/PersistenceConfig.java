package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing(auditorAwareRef = "authAuditorAware")
@Configuration
public class PersistenceConfig {
}
