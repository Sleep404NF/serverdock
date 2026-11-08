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

    /**
     * 이 역할이 other보다 높은지. 같은 역할이면 false.
     */
    public boolean outranks(ServerRole other) {
        return this.ordinal() < other.ordinal();
    }
}
