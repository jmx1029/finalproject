package com.bookstore.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI bookstoreOpenAPI() {
        String schemeName = "BearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("📚 在线书店 API 文档")
                        .version("1.0.0")
                        .description("毕业设计项目 —— Spring Boot + Vue3 在线书店系统接口文档")
                        .contact(new Contact().name("Bookstore Team")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .schemaRequirement(schemeName, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .bearerFormat("JWT")
                        .scheme("bearer")
                        .description("登录后获取的 token，格式：Bearer xxx"));
    }
}
