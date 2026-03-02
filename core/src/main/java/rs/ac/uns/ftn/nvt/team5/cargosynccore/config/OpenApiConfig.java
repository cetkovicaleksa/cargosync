package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CargoSync API")
                        .version("v1")
                        .description("REST API for the CargoSync logistics platform — warehouse management, orders, products and customer accounts.")
                        .contact(new Contact().name("Team 5 — NVT FTN UNS"))
                )
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT access token (obtained from POST /auth/login)")
                        )
                )
                .tags(List.of(
                        new Tag().name("Auth").description("Login, logout, password change"),
                        new Tag().name("Customer Registration").description("Self-registration and email activation"),
                        new Tag().name("Warehouses").description("Warehouse CRUD and temperature analytics (Manager)"),
                        new Tag().name("Products").description("Product catalogue and management"),
                        new Tag().name("Orders").description("Order creation and history (Customer)"),
                        new Tag().name("Companies").description("Customer company management")
                ));
    }
}
