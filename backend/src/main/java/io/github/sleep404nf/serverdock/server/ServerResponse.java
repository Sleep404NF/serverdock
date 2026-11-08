package io.github.sleep404nf.serverdock.server;

import java.time.LocalDateTime;

public record ServerResponse(
        Long id,
        String name,
        String gameVersion,
        ServerLoader loader,
        String loaderVersion,
        int memoryMb,
        int port,
        boolean autoRestart,
        ModpackSource modpackSource,
        String modpackRef,
        ServerRole myRole,
        LocalDateTime createdAt
) {
    public static ServerResponse of(Server server, ServerRole myRole) {
        return new ServerResponse(
                server.getId(),
                server.getName(),
                server.getGameVersion(),
                server.getLoader(),
                server.getLoaderVersion(),
                server.getMemoryMb(),
                server.getPort(),
                server.isAutoRestart(),
                server.getModpackSource(),
                server.getModpackRef(),
                myRole,
                server.getCreatedAt()
        );
    }
}
