package com.c102.ourotail.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 설정 클래스
 * API 문서화를 위한 설정을 제공합니다.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ourotail SDK API")
                        .description("K6 성능 테스트 및 기본 테스트 API를 제공하는 Ourotail SDK")
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("Ourotail Team")
                                .email("contact@ourotail.com")
                                .url("https://github.com/ourotail/ourotail-sdk"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.txt")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("로컬 개발 서버"),
                        new Server()
                                .url("https://api.ourotail.com")
                                .description("프로덕션 서버")
                ));
    }
}
