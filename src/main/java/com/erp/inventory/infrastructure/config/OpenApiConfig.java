package com.erp.inventory.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI inventoryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ERP Inventory Service API")
                        .description("Microservicio de Catálogo de Repuestos, Stock y Almacenes — DriveOS ERP")
                        .version("v1.0.0"));
    }
}
