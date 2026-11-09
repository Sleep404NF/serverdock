package io.github.sleep404nf.serverdock.server;

import io.github.sleep404nf.serverdock.docker.MinecraftContainerManager;
import io.github.sleep404nf.serverdock.docker.MinecraftServerSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerPowerService {

    private final ServerAccessChecker serverAccessChecker;
    private final MinecraftContainerManager containerManager;

    /**
     * 서버 시작. 멤버라면 누구나 켤 수 있고, 처음 켤 때 컨테이너를 만든다.
     */
    @Transactional
    public ServerStatusResponse start(Long userId, Long serverId) {
        Server server = serverAccessChecker.require(serverId, userId, ServerRole.MEMBER).getServer();

        server.attachContainer(containerManager.start(toSpec(server)));
        return statusOf(server);
    }

    /**
     * 서버 중지. ADMIN 이상만 가능하다.
     */
    public ServerStatusResponse stop(Long userId, Long serverId) {
        Server server = serverAccessChecker.require(serverId, userId, ServerRole.ADMIN).getServer();

        containerManager.stop(server.getId());
        return statusOf(server);
    }

    /**
     * 서버 재시작. ADMIN 이상만 가능하고, 바뀐 설정이 있으면 이때 적용된다.
     */
    @Transactional
    public ServerStatusResponse restart(Long userId, Long serverId) {
        Server server = serverAccessChecker.require(serverId, userId, ServerRole.ADMIN).getServer();

        server.attachContainer(containerManager.restart(toSpec(server)));
        return statusOf(server);
    }

    /**
     * 서버 상태. 멤버라면 누구나 볼 수 있다.
     */
    public ServerStatusResponse status(Long userId, Long serverId) {
        Server server = serverAccessChecker.require(serverId, userId, ServerRole.MEMBER).getServer();
        return statusOf(server);
    }

    private ServerStatusResponse statusOf(Server server) {
        return new ServerStatusResponse(
                server.getId(),
                containerManager.status(server.getId()),
                containerManager.isOutdated(toSpec(server))
        );
    }

    private MinecraftServerSpec toSpec(Server server) {
        return new MinecraftServerSpec(
                server.getId(),
                server.getPort(),
                server.getLoader().name(),
                server.getGameVersion(),
                server.getLoaderVersion(),
                server.getMemoryMb()
        );
    }
}
