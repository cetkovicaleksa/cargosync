package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.filter.AuthFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.ratelimit.LoginRateLimitFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties.CorsProperties;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties.SecurityProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.function.Predicate.not;
import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
class SecurityConfig {
    private static final String TAG = "SecurityConfig";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            CorsProperties corsProperties
    ) {
        var config = new CorsConfiguration();

        config.setAllowedOriginPatterns(corsProperties.allowedOrigins());
        config.setAllowedMethods(corsProperties.allowedMethods());
        config.setAllowedHeaders(corsProperties.allowedHeaders());
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthFilter authFilter,
            LoginRateLimitFilter loginRateLimitFilter,
            SecurityProperties sp
            ) throws Exception
    {
        log.info("Exposing endpoints matching {}", Arrays.toString(sp.publicEndpoints()));

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(sp.publicEndpoints()).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginRateLimitFilter, AuthFilter.class);

        return http.build();
    }

}
