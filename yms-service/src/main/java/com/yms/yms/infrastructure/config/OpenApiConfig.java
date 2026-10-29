package com.yms.yms.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI ymsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("YMS API")
                        .description("API quản lý bãi xe, dịch vụ, đối tác, hợp đồng, hóa đơn và theo dõi GPS. "
                                + "Đăng nhập qua /api/auth/login, sau đó dùng nút Authorize để nhập JWT.")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập access token JWT từ API đăng nhập.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
