package io.github.sleep404nf.serverdock.server;

import io.github.sleep404nf.serverdock.common.BusinessException;
import io.github.sleep404nf.serverdock.user.User;
import io.github.sleep404nf.serverdock.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerService {

    private static final int DEFAULT_PORT = 25565;
    private static final int MAX_PORT = 65535;

    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final UserRepository userRepository;
    private final ServerAccessChecker serverAccessChecker;

    /**
     * 서버 생성. 시스템 관리자만 가능하고, 만든 사람이 OWNER가 된다.
     */
    @Transactional
    public ServerResponse create(Long userId, CreateServerRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        if (!user.isAdmin()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "서버는 관리자만 만들 수 있습니다.");
        }

        String name = request.name().strip();
        if (serverRepository.existsByName(name)) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 사용 중인 서버 이름입니다: " + name);
        }

        int port = request.port() != null ? request.port() : findFreePort();
        if (serverRepository.existsByPort(port)) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 사용 중인 포트입니다: " + port);
        }

        Server server = serverRepository.save(Server.create(
                name,
                user,
                request.gameVersion(),
                request.loader(),
                request.loaderVersion(),
                request.memoryMb(),
                port
        ));
        ServerMember owner = serverMemberRepository.save(ServerMember.create(server, user, ServerRole.OWNER));

        return ServerResponse.of(server, owner.getRole());
    }

    private int findFreePort() {
        Set<Integer> usedPorts = new HashSet<>(serverRepository.findAllPorts());
        for (int port = DEFAULT_PORT; port <= MAX_PORT; port++) {
            if (!usedPorts.contains(port)) {
                return port;
            }
        }
        throw new BusinessException(HttpStatus.CONFLICT, "사용 가능한 포트가 없습니다.");
    }

    /**
     * 내가 멤버로 속한 서버 목록
     */
    public List<ServerResponse> findMyServers(Long userId) {
        return serverMemberRepository.findAllByUserIdWithServer(userId).stream()
                .map(member -> ServerResponse.of(member.getServer(), member.getRole()))
                .toList();
    }

    /**
     * 서버 상세. 멤버라면 누구나 볼 수 있다.
     */
    public ServerResponse findOne(Long userId, Long serverId) {
        ServerMember member = serverAccessChecker.require(serverId, userId, ServerRole.MEMBER);
        return ServerResponse.of(member.getServer(), member.getRole());
    }

    /**
     * 서버 설정 변경. OWNER만 가능하고, 보낸 값만 바꾼다.
     */
    @Transactional
    public ServerResponse update(Long userId, Long serverId, UpdateServerRequest request) {
        ServerMember member = serverAccessChecker.require(serverId, userId, ServerRole.OWNER);
        Server server = member.getServer();

        if (request.name() != null) {
            String name = request.name().strip();
            if (serverRepository.existsByNameAndIdNot(name, serverId)) {
                throw new BusinessException(HttpStatus.CONFLICT, "이미 사용 중인 서버 이름입니다: " + name);
            }
            server.rename(name);
        }
        if (request.memoryMb() != null) {
            server.changeMemory(request.memoryMb());
        }
        if (request.autoRestart() != null) {
            server.changeAutoRestart(request.autoRestart());
        }

        return ServerResponse.of(server, member.getRole());
    }

    /**
     * 서버 삭제. OWNER만 가능하다.
     */
    @Transactional
    public void delete(Long userId, Long serverId) {
        serverAccessChecker.require(serverId, userId, ServerRole.OWNER);

        serverMemberRepository.deleteAllByServerId(serverId);
        serverRepository.deleteById(serverId);
    }
}
