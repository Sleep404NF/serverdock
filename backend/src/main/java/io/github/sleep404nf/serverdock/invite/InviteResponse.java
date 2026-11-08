package io.github.sleep404nf.serverdock.invite;

import io.github.sleep404nf.serverdock.server.ServerRole;

import java.time.LocalDateTime;

public record InviteResponse(
        Long id,
        String code,
        ServerRole role,
        String createdBy,
        LocalDateTime expiresAt,
        Integer maxUses,
        int usedCount,
        boolean usable,
        LocalDateTime createdAt
) {
    public static InviteResponse from(Invite invite) {
        return new InviteResponse(
                invite.getId(),
                invite.getCode(),
                invite.getRole(),
                invite.getCreatedBy().getUsername(),
                invite.getExpiresAt(),
                invite.getMaxUses(),
                invite.getUsedCount(),
                !invite.isExpired(LocalDateTime.now()) && !invite.isUsedUp(),
                invite.getCreatedAt()
        );
    }
}
