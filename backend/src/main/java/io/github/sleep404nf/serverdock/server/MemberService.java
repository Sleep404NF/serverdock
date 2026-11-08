package io.github.sleep404nf.serverdock.server;

import io.github.sleep404nf.serverdock.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final ServerMemberRepository serverMemberRepository;
    private final ServerAccessChecker serverAccessChecker;

    /**
     * 멤버 목록. 역할 순(OWNER, ADMIN, MEMBER), 같은 역할은 먼저 들어온 순.
     */
    public List<MemberResponse> findAll(Long userId, Long serverId) {
        serverAccessChecker.require(serverId, userId, ServerRole.MEMBER);

        return serverMemberRepository.findAllByServerIdWithUser(serverId).stream()
                .sorted(Comparator.comparing(ServerMember::getRole)
                        .thenComparing(ServerMember::getJoinedAt))
                .map(MemberResponse::from)
                .toList();
    }

    /**
     * 역할 변경. OWNER만 가능하고, ADMIN과 MEMBER 사이로만 바꿀 수 있다.
     */
    @Transactional
    public MemberResponse changeRole(Long userId, Long serverId, Long targetUserId, UpdateMemberRoleRequest request) {
        serverAccessChecker.require(serverId, userId, ServerRole.OWNER);

        if (request.role() == ServerRole.OWNER) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "OWNER로는 바꿀 수 없습니다.");
        }

        ServerMember target = findMember(serverId, targetUserId);
        if (target.getRole() == ServerRole.OWNER) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "OWNER의 역할은 바꿀 수 없습니다.");
        }

        target.changeRole(request.role());
        return MemberResponse.from(target);
    }

    /**
     * 내보내기 또는 나가기. 자기 자신이면 나가기, 다른 사람이면 자기보다 낮은 역할만 내보낼 수 있다.
     */
    @Transactional
    public void remove(Long userId, Long serverId, Long targetUserId) {
        ServerMember me = serverAccessChecker.require(serverId, userId, ServerRole.MEMBER);

        if (userId.equals(targetUserId)) {
            if (me.getRole() == ServerRole.OWNER) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "OWNER는 서버를 나갈 수 없습니다. 서버를 삭제해주세요.");
            }
            serverMemberRepository.delete(me);
            return;
        }

        if (!me.hasRoleAtLeast(ServerRole.ADMIN)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "작업 권한이 없습니다.");
        }

        ServerMember target = findMember(serverId, targetUserId);
        if (!me.getRole().outranks(target.getRole())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "자신보다 낮은 역할의 멤버만 내보낼 수 있습니다.");
        }

        serverMemberRepository.delete(target);
    }

    private ServerMember findMember(Long serverId, Long userId) {
        return serverMemberRepository.findById(new ServerMemberId(serverId, userId))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다."));
    }
}
