package io.github.sleep404nf.serverdock.server;

import io.github.sleep404nf.serverdock.common.BusinessException;
import io.github.sleep404nf.serverdock.docker.MinecraftContainerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 서버 휴지통. 지운 서버는 보관 기간 동안 복구할 수 있고, 기간이 지나면 완전히 지워진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerTrashService {

    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final ServerAccessChecker serverAccessChecker;
    private final MinecraftContainerManager containerManager;
    private final TrashProperties trashProperties;

    /**
     * 서버를 휴지통으로 옮긴다. OWNER만 가능하다. 서버는 끄고, 보관 기간 동안 이름과 포트는 잡아둔다.
     */
    @Transactional
    public void moveToTrash(Long userId, Long serverId) {
        Server server = serverAccessChecker.require(serverId, userId, ServerRole.OWNER).getServer();

        containerManager.stop(server.getId());
        server.moveToTrash(LocalDateTime.now());
    }

    /**
     * 휴지통에 있는 내 서버(OWNER인 것만). 최근에 지운 것부터.
     */
    public List<TrashedServerResponse> findTrash(Long userId) {
        return serverMemberRepository.findAllInTrashByUserIdAndRole(userId, ServerRole.OWNER).stream()
                .map(member -> TrashedServerResponse.of(member.getServer(), trashProperties.retention()))
                .toList();
    }

    /**
     * 휴지통에서 꺼낸다. OWNER만 가능하고, 서버는 꺼진 상태로 돌아온다.
     */
    @Transactional
    public ServerResponse restore(Long userId, Long serverId) {
        ServerMember member = serverAccessChecker.requireIncludingTrash(serverId, userId, ServerRole.OWNER);
        Server server = member.getServer();

        if (!server.isInTrash()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "휴지통에 있는 서버가 아닙니다.");
        }

        server.restore();
        return ServerResponse.of(server, member.getRole());
    }

    /**
     * 보관 기간이 지난 서버를 완전히 지운다. 컨테이너와 월드까지 지우므로 되돌릴 수 없다.
     * 한 서버에서 실패해도 나머지는 계속 지우고, 실패한 서버는 다음 실행 때 다시 시도한다.
     */
    @Transactional
    public int purgeExpired() {
        LocalDateTime threshold = LocalDateTime.now().minus(trashProperties.retention());
        List<Long> expiredIds = serverRepository.findAllInTrashDeletedBefore(threshold).stream()
                .map(Server::getId)
                .toList();

        int purged = 0;
        for (Long serverId : expiredIds) {
            try {
                containerManager.removeServer(serverId);
            } catch (RuntimeException e) {
                log.error("서버 {}의 컨테이너를 지우지 못해 다음에 다시 시도합니다.", serverId, e);
                continue;
            }
            serverMemberRepository.deleteAllByServerId(serverId);
            serverRepository.deleteById(serverId);
            purged++;
        }
        return purged;
    }
}
