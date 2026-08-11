package com.uptimepulse.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI uptimePulseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UptimePulse SaaS API")
                        .version("1.0.0")
                        .description("Secure website monitoring, latency, and SSL expiry API")
                        .contact(new Contact().name("UptimePulse Support").email("support@uptimepulse.com"))
                );
    }
}
