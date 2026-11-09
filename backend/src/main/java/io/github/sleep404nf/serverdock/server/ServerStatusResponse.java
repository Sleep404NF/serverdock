package io.github.sleep404nf.serverdock.server;

import io.github.sleep404nf.serverdock.docker.ServerStatus;

public record ServerStatusResponse(
        Long serverId,
        ServerStatus status
) {
}