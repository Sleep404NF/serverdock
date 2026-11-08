package io.github.sleep404nf.serverdock.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 설정. Authorize 버튼에 로그인 토큰을 넣으면 모든 요청에 Bearer 헤더가 붙는다.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "ServerDock API",
                version = "v0",
                description = "친구끼리 쓰는 마인크래프트 서버 관리 도구"
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}