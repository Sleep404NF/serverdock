package io.github.sleep404nf.serverdock.server;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateServerRequest(
        @Size(min = 1, max = 50, message = "서버 이름은 1~50자여야 합니다.")
        @Pattern(regexp = ".*\\S.*", message = "서버 이름은 공백만으로 만들 수 없습니다.")
        String name,

        @Min(value = 512, message = "메모리는 512MB 이상이어야 합니다.")
        @Max(value = 65536, message = "메모리는 65536MB 이하여야 합니다.")
        Integer memoryMb,

        Boolean autoRestart
) {
}