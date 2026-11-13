package io.github.sleep404nf.serverdock.user;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        SystemRole systemRole,
        boolean discordLinked,
        String discordUsername,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getSystemRole(),
                user.getDiscordUserId() != null,
                user.getDiscordUsername(),
                user.getCreatedAt()
        );
    }
}
