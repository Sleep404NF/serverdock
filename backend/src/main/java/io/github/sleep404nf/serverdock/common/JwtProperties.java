package io.github.sleep404nf.serverdock.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * application.yml의 serverdock.jwt 설정
 */
@ConfigurationProperties(prefix = "serverdock.jwt")
public record JwtProperties(String secret, Duration expiry) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("serverdock.jwt.secret은 32바이트 이상이어야 합니다.");
        }
        if (expiry == null) {
            expiry = Duration.ofHours(12);
        }
    }

    public SecretKey secretKey() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
