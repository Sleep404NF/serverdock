package io.github.sleep404nf.serverdock.server;

import java.time.LocalDateTime;

public record MemberResponse(
        Long userId,
        String username,
        ServerRole role,
        LocalDateTime joinedAt
) {
    public static MemberResponse from(ServerMember member) {
        return new MemberResponse(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
