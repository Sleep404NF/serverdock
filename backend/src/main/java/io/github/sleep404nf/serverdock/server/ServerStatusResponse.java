package io.github.sleep404nf.serverdock.server;

import io.github.sleep404nf.serverdock.docker.ServerStatus;

/**
 * restartRequired가 true면 켜져 있는 동안 설정이 바뀐 것이라, 재시작해야 적용된다.
 */
public record ServerStatusResponse(
        Long serverId,
        ServerStatus status,
        boolean restartRequired
) {
}
