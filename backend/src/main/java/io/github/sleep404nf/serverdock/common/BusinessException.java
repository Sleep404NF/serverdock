package io.github.sleep404nf.serverdock.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 예상된 업무 오류. 상태 코드와 메시지를 그대로 응답한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
