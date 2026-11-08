package io.github.sleep404nf.serverdock.server;

/**
 * 서버 멤버의 역할. 아래로 갈수록 권한이 좁다.
 */
public enum ServerRole {
    OWNER,
    ADMIN,
    MEMBER;

    /**
     * 이 역할이 required 이상의 권한을 가졌는지
     */
    public boolean isAtLeast(ServerRole required) {
        return this.ordinal() <= required.ordinal();
    }
}
