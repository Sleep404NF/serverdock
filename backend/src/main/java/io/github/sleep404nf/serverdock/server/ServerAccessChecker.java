package io.github.sleep404nf.serverdock.server;

import io.github.sleep404nf.serverdock.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServerAccessChecker {

    private final ServerMemberRepository serverMemberRepository;

    public ServerMember require(Long serverId, Long userId, ServerRole required) {
        ServerMember member = serverMemberRepository.findById(new ServerMemberId(serverId, userId))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다."));
        if (!member.hasRoleAtLeast(required)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "작업 권한이 없습니다.");
        }

        return member;
    }
}