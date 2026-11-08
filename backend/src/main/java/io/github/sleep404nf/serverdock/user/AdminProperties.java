package io.github.sleep404nf.serverdock.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 serverdock.admin 설정. 서버 시작 시 관리자 계정을 만들 때 쓴다.
 */
@ConfigurationProperties(prefix = "serverdock.admin")
public record AdminProperties(String username, String password) {

    public AdminProperties {
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("serverdock.admin.username을 설정해주세요.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("serverdock.admin.password를 설정해주세요.");
        }
    }
}
