package io.github.sleep404nf.serverdock.server;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateServerRequest(
        @NotBlank(message = "서버 이름을 입력해주세요.")
        @Size(max = 50, message = "서버 이름은 50자 이하여야 합니다.")
        String name,

        @NotBlank(message = "게임 버전을 입력해주세요.")
        @Size(max = 32, message = "게임 버전은 32자 이하여야 합니다.")
        @Pattern(regexp = "^[0-9A-Za-z._-]+$", message = "게임 버전 형식이 올바르지 않습니다.")
        String gameVersion,

        @NotNull(message = "로더를 선택해주세요.")
        ServerLoader loader,

        @Size(max = 32, message = "로더 버전은 32자 이하여야 합니다.")
        String loaderVersion,

        @Min(value = 512, message = "메모리는 512MB 이상이어야 합니다.")
        @Max(value = 65536, message = "메모리는 65536MB 이하여야 합니다.")
        int memoryMb,

        @Min(value = 1024, message = "포트는 1024 이상이어야 합니다.")
        @Max(value = 65535, message = "포트는 65535 이하여야 합니다.")
        Integer port
) {
}
