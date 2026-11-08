package io.github.sleep404nf.serverdock.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        @Size(min = 6, max = 20, message = "아이디는 6~20자여야 합니다.")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "아이디는 영문, 숫자, 밑줄(_)만 쓸 수 있습니다.")
        String username,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다.")
        String password
) {
}
