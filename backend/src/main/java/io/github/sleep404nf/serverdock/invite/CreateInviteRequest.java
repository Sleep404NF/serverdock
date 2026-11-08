package io.github.sleep404nf.serverdock.invite;

import io.github.sleep404nf.serverdock.server.ServerRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateInviteRequest(
        @NotNull(message = "초대할 역할을 선택해주세요.")
        ServerRole role,

        InviteExpiry expiresIn,

        @Min(value = 1, message = "사용 횟수는 1회 이상이어야 합니다.")
        @Max(value = 100, message = "사용 횟수는 100회 이하여야 합니다.")
        Integer maxUses
) {
}
