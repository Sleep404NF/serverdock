package io.github.sleep404nf.serverdock.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import io.github.sleep404nf.serverdock.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * 마인크래프트 서버 콘솔. 컨테이너 로그를 따라 읽고, rcon-cli로 명령어를 보낸다.
 * itzg/minecraft-server 이미지는 RCON이 기본으로 켜져 있고 컨테이너 안에 rcon-cli가 들어 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinecraftConsole {

    private static final int COMMAND_TIMEOUT_SECONDS = 10;
    /** 터미널 색상 코드 (ESC[...m) */
    private static final Pattern ANSI_CODE = Pattern.compile("\u001B\\[[0-9;?]*[A-Za-z]");
    /** 마인크래프트 색상 코드 (§a, §l 등) */
    private static final Pattern FORMATTING_CODE = Pattern.compile("§[0-9A-FK-ORa-fk-or]");
    /** 명령어를 보낼 때마다 RCON이 남기는 연결/종료 줄. 콘솔에서는 잡음이라 뺀다. */
    private static final Pattern RCON_CONNECTION_LINE =
            Pattern.compile("^\\[[^\\]]*] \\[RCON (Listener|Client)[^\\]]*]: Thread RCON Client \\S+ (started|shutting down)$");

    private final DockerClient dockerClient;

    /**
     * 로그를 따라간다. 지난 로그 tail줄을 먼저 보내고, 그 뒤로 새 줄이 생길 때마다 onLine을 부른다.
     * 서버가 꺼지거나 읽기가 끊기면 onEnd를 부른다. 돌려준 Closeable을 닫으면 읽기를 멈춘다.
     */
    public Closeable followLogs(long serverId, int tail, Consumer<String> onLine, Runnable onEnd) {
        return dockerClient.logContainerCmd(MinecraftContainerManager.containerName(serverId))
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .withTail(tail)
                .exec(new LogCallback(onLine, onEnd));
    }

    /**
     * 명령어를 보내고 서버의 응답을 돌려준다. 서버가 완전히 켜져 있어야 한다.
     */
    public String execute(long serverId, String command) {
        ExecCreateCmdResponse exec = dockerClient.execCreateCmd(MinecraftContainerManager.containerName(serverId))
                .withCmd("rcon-cli", command)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        OutputCollector output = new OutputCollector();
        try (output) {
            boolean finished = dockerClient.execStartCmd(exec.getId())
                    .exec(output)
                    .awaitCompletion(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                throw new BusinessException(HttpStatus.GATEWAY_TIMEOUT,
                        "서버가 " + COMMAND_TIMEOUT_SECONDS + "초 안에 응답하지 않았습니다.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("명령어를 보내는 중에 중단됐습니다.", e);
        } catch (IOException e) {
            log.debug("명령어 출력 스트림을 닫지 못했습니다.", e);
        }

        String result = clean(output.text()).strip();
        Long exitCode = dockerClient.inspectExecCmd(exec.getId()).exec().getExitCodeLong();
        if (exitCode != null && exitCode != 0) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "명령어를 보내지 못했습니다: " + result);
        }
        return result;
    }

    static String clean(String text) {
        String withoutAnsi = ANSI_CODE.matcher(text).replaceAll("");
        return FORMATTING_CODE.matcher(withoutAnsi).replaceAll("");
    }

    /**
     * 로그 조각(Frame)을 줄 단위로 잘라서 넘긴다. 한 조각에 여러 줄이 오거나 줄이 잘려 올 수 있다.
     */
    private static final class LogCallback extends ResultCallback.Adapter<Frame> {

        private final Consumer<String> onLine;
        private final Runnable onEnd;
        private final StringBuilder buffer = new StringBuilder();

        private LogCallback(Consumer<String> onLine, Runnable onEnd) {
            this.onLine = onLine;
            this.onEnd = onEnd;
        }

        @Override
        public void onNext(Frame frame) {
            buffer.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
            int newline;
            while ((newline = buffer.indexOf("\n")) >= 0) {
                String line = buffer.substring(0, newline);
                buffer.delete(0, newline + 1);
                emit(line);
            }
        }

        @Override
        public void onComplete() {
            if (!buffer.isEmpty()) {
                emit(buffer.toString());
                buffer.setLength(0);
            }
            super.onComplete();
            onEnd.run();
        }

        private void emit(String rawLine) {
            String line = clean(rawLine.stripTrailing());
            if (!RCON_CONNECTION_LINE.matcher(line).matches()) {
                onLine.accept(line);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            log.debug("로그 읽기가 끊겼습니다: {}", throwable.toString());
            super.onError(throwable);
            onEnd.run();
        }
    }

    private static final class OutputCollector extends ResultCallback.Adapter<Frame> {

        private final StringBuilder text = new StringBuilder();

        @Override
        public synchronized void onNext(Frame frame) {
            text.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
        }

        synchronized String text() {
            return text.toString();
        }
    }
}
