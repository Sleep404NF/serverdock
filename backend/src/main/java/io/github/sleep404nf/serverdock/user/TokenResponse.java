package io.github.sleep404nf.serverdock.user;

/**
 * expiresIn은 초 단위
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
