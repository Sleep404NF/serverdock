package io.github.sleep404nf.serverdock.docker;

/**
 * 마인크래프트 서버 컨테이너의 상태
 */
public enum ServerStatus {
    NOT_CREATED,
    STOPPED,
    STARTING,
    RUNNING
}