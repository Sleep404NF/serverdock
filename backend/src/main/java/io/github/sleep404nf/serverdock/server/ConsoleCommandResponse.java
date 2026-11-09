package io.github.sleep404nf.serverdock.server;

/**
 * 보낸 명령어와 서버의 응답. 응답이 없는 명령어면 output은 빈 문자열이다.
 */
public record ConsoleCommandResponse(String command, String output) {
}
