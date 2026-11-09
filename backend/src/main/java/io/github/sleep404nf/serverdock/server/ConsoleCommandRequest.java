package io.github.sleep404nf.serverdock.server;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 콘솔 명령어. 앞에 /를 붙여도 되고 안 붙여도 된다.
 */
public record ConsoleCommandRequest(
        @NotBlank(message = "명령어를 입력해주세요.")
        @Size(max = 256, message = "명령어는 256자 이하여야 합니다.")
        @Pattern(regexp = "^[^\\r\\n]*$", message = "명령어는 한 줄로 입력해주세요.")
        String command
) {
}
