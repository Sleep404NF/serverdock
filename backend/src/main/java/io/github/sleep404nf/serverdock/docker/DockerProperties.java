package io.github.sleep404nf.serverdock.docker;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 serverdock.docker 설정
 */
@ConfigurationProperties(prefix = "serverdock.docker")
public record DockerProperties(String host, String image) {

    public DockerProperties {
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("serverdock.docker.host를 설정해주세요.");
        }
        if (image == null || image.isBlank()) {
            throw new IllegalStateException("serverdock.docker.image를 설정해주세요.");
        }
    }
}