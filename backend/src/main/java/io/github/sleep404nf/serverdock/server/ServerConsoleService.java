package io.github.sleep404nf.serverdock.server;

import io.github.sleep404nf.serverdock.common.BusinessException;
import io.github.sleep404nf.serverdock.docker.MinecraftConsole;
import io.github.sleep404nf.serverdock.docker.MinecraftContainerManager;
import io.github.sleep404nf.serverdock.docker.ServerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 실시간 콘솔. 로그 보기는 멤버 누구나, 명령어는 ADMIN 이상만 가능하다.
 * Docker를 오래 붙잡는 작업이라 DB 트랜잭션은 열지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServerConsoleService {

    private static final int LOG_TAIL_LINES = 200;
    private static final long NO_TIMEOUT = 0L;
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(20);

    private final ServerAccessChecker serverAccessChecker;
    private final MinecraftContainerManager containerManager;
    private final MinecraftConsole minecraftConsole;
    private final TaskScheduler taskScheduler;

    /**
     * 로그를 SSE로 흘려보낸다. 지난 로그 200줄을 먼저 보내고 이후 새 줄을 실시간으로 보낸다.
     * 이벤트는 log(로그 한 줄)와 end(서버가 꺼져 스트림이 끝남) 두 가지다.
     */
    public SseEmitter streamLogs(Long userId, Long serverId) {
        serverAccessChecker.require(serverId, userId, ServerRole.MEMBER);
        if (containerManager.status(serverId) == ServerStatus.NOT_CREATED) {
            throw new BusinessException(HttpStatus.CONFLICT, "아직 한 번도 켜지 않은 서버라 로그가 없습니다.");
        }

        LogSession session = new LogSession(new SseEmitter(NO_TIMEOUT));
        session.start(serverId);
        return session.emitter;
    }

    /**
     * 명령어를 보낸다. ADMIN 이상만 가능하고, 서버가 완전히 켜져 있어야 한다.
     */
    public ConsoleCommandResponse execute(Long userId, Long serverId, ConsoleCommandRequest request) {
        serverAccessChecker.require(serverId, userId, ServerRole.ADMIN);
        if (containerManager.status(serverId) != ServerStatus.RUNNING) {
            throw new BusinessException(HttpStatus.CONFLICT, "서버가 완전히 켜진 뒤에 명령어를 보낼 수 있습니다.");
        }

        String command = request.command().strip();
        if (command.startsWith("/")) {
            command = command.substring(1).strip();
        }
        if (command.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "명령어를 입력해주세요.");
        }
        // 콘솔에서 stop으로 끄면 자동 재시작이 켜진 서버는 Docker가 바로 다시 켜고, 서버 상태도 꼬인다.
        if (command.equalsIgnoreCase("stop")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "서버를 끄려면 중지 버튼을 써주세요.");
        }

        log.info("콘솔 명령어: 서버 {}, 사용자 {}: {}", serverId, userId, command);
        return new ConsoleCommandResponse(command, minecraftConsole.execute(serverId, command));
    }

    /**
     * 브라우저 연결 하나. 연결이 끊기면 Docker 로그 읽기와 heartbeat를 함께 정리한다.
     * heartbeat는 아무 로그가 없을 때도 끊긴 연결을 알아채고, 중간 프록시가 연결을 닫지 않게 한다.
     */
    private final class LogSession {

        private final SseEmitter emitter;
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private volatile Closeable logs;
        private volatile ScheduledFuture<?> heartbeat;

        private LogSession(SseEmitter emitter) {
            this.emitter = emitter;
            emitter.onCompletion(this::release);
            emitter.onTimeout(this::release);
            emitter.onError(error -> release());
        }

        private void start(long serverId) {
            logs = minecraftConsole.followLogs(serverId, LOG_TAIL_LINES,
                    line -> send(SseEmitter.event().name("log").data(line)),
                    this::finish);
            heartbeat = taskScheduler.scheduleAtFixedRate(
                    () -> send(SseEmitter.event().comment("ping")), HEARTBEAT_INTERVAL);
            if (closed.get()) {
                release();
            }
        }

        private void send(SseEmitter.SseEventBuilder event) {
            if (closed.get()) {
                return;
            }
            try {
                emitter.send(event);
            } catch (IOException | IllegalStateException e) {
                release();
            }
        }

        /** 서버가 꺼져 로그가 끝났다. 브라우저가 먼저 끊었으면 할 일이 없다. */
        private void finish() {
            if (closed.get()) {
                return;
            }
            send(SseEmitter.event().name("end").data("서버가 꺼졌습니다."));
            release();
            emitter.complete();
        }

        private void release() {
            closed.set(true);
            ScheduledFuture<?> currentHeartbeat = heartbeat;
            if (currentHeartbeat != null) {
                currentHeartbeat.cancel(false);
            }
            Closeable currentLogs = logs;
            if (currentLogs != null) {
                try {
                    currentLogs.close();
                } catch (IOException e) {
                    log.debug("로그 스트림을 닫지 못했습니다.", e);
                }
            }
        }
    }
}
