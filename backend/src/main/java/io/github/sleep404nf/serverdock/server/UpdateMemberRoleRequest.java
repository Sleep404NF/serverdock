package io.github.sleep404nf.serverdock.server;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "바꿀 역할을 선택해주세요.")
        ServerRole role
) {
}
