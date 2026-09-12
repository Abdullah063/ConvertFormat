package com.altun.convertformat.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI convertFormatOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ConvertFormat API")
                .description("DOCX dosyalarını güvenli biçimde PDF'e dönüştürür.")
                .version("v1"));
    }
}
