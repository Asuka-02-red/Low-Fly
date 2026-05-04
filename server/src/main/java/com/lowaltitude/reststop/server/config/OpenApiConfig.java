package com.lowaltitude.reststop.server.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger配置类。
 * <p>
 * 配置API文档的基本信息（标题、版本、描述、联系方式），
 * 并定义Bearer JWT安全认证方案，使Swagger UI支持在线调试需鉴权的接口。
 * </p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lowAltitudeOpenApi() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("低空驿站 API")
                        .version("1.0.0")
                        .description("演示版后端接口")
                        .contact(new Contact().name("低空驿站团队")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .schemaRequirement(schemeName, new SecurityScheme()
                        .name(schemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .externalDocs(new ExternalDocumentation().description("项目规范").url("/规范.md"));
    }
}
