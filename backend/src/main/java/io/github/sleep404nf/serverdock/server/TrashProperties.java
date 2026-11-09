package io.github.sleep404nf.serverdock.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * application.yml의 serverdock.trash 설정. retention은 휴지통 보관 기간이다.
 */
@ConfigurationProperties(prefix = "serverdock.trash")
public record TrashProperties(Duration retention) {

    public TrashProperties {
        if (retention == null) {
            retention = Duration.ofDays(2);
        }
    }
}
