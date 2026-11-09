package io.github.sleep404nf.serverdock.server;

import io.github.sleep404nf.serverdock.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서버별 권한 검사. 멤버가 아니면 404, 역할이 부족하면 403.
 * 휴지통에 있는 서버는 없는 서버처럼 404로 응답한다.
 * 트랜잭션 밖(실시간 콘솔)에서 불려도 서버 정보를 읽을 수 있게 자체 트랜잭션을 연다.
 */
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ServerAccessChecker {

    private final ServerMemberRepository serverMemberRepository;

    public ServerMember require(Long serverId, Long userId, ServerRole required) {
        ServerMember member = requireIncludingTrash(serverId, userId, required);
        if (member.getServer().isInTrash()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다.");
        }
        return member;
    }

    /**
     * 휴지통에 있는 서버까지 포함해서 확인한다. 복구할 때만 쓴다.
     */
    public ServerMember requireIncludingTrash(Long serverId, Long userId, ServerRole required) {
        ServerMember member = serverMemberRepository.findById(new ServerMemberId(serverId, userId))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다."));
        if (!member.hasRoleAtLeast(required)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "작업 권한이 없습니다.");
        }
        return member;
    }
}
