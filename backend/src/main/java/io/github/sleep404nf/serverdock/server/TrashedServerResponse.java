package io.github.sleep404nf.serverdock.server;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 휴지통에 있는 서버. purgeAt이 지나면 컨테이너와 월드까지 완전히 지워진다.
 */
public record TrashedServerResponse(
        Long id,
        String name,
        int port,
        LocalDateTime deletedAt,
        LocalDateTime purgeAt
) {
    public static TrashedServerResponse of(Server server, Duration retention) {
        return new TrashedServerResponse(
                server.getId(),
                server.getName(),
                server.getPort(),
                server.getDeletedAt(),
                server.getDeletedAt().plus(retention)
        );
    }
}
