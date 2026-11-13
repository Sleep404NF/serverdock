package io.github.sleep404nf.serverdock.discord;

import io.github.sleep404nf.serverdock.docker.ContainerSnapshot;
import io.github.sleep404nf.serverdock.docker.MinecraftContainerManager;
import io.github.sleep404nf.serverdock.docker.ServerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 알림 채널이 지정된 서버의 상태를 10초마다 확인하고, 바뀌면 디스코드로 알린다.
 * 웹, 디스코드, 자동 재시작 중 무엇으로 바뀌었든 실제 컨테이너 상태를 보고 판단한다.
 * 백엔드가 막 켜졌을 때는 지금 상태만 기억하고 알리지 않는다(재시작할 때마다 알림이 쏟아지지 않게).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServerStatusNotifier {

    /** 정상 종료로 보는 종료 코드. 143은 Docker가 보낸 종료 신호(SIGTERM)로 끝난 경우다. */
    private static final Set<Long> NORMAL_EXIT_CODES = Set.of(0L, 143L);

    private final DiscordBot discordBot;
    private final DiscordLinkRepository discordLinkRepository;
    private final MinecraftContainerManager containerManager;
    private final Map<Long, ContainerSnapshot> lastSeen = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 10, initialDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void check() {
        Optional<JDA> jda = discordBot.connected();
        if (jda.isEmpty()) {
            return;
        }

        List<DiscordLink> links = discordLinkRepository.findAllActiveWithServer();
        Set<Long> watching = new HashSet<>();
        for (DiscordLink link : links) {
            long serverId = link.getServer().getId();
            watching.add(serverId);

            ContainerSnapshot now;
            try {
                now = containerManager.snapshot(serverId);
            } catch (RuntimeException e) {
                log.debug("서버 {} 상태를 읽지 못했습니다.", serverId, e);
                continue;
            }

            ContainerSnapshot before = lastSeen.put(serverId, now);
            if (before == null) {
                continue;
            }
            String message = describeChange(link.getServer().getName(), link.getServer().getPort(), before, now);
            if (message != null) {
                send(jda.get(), link.getChannelId(), message);
            }
        }
        // 알림을 끈 서버나 지운 서버는 더 이상 기억하지 않는다
        lastSeen.keySet().retainAll(watching);
    }

    /**
     * 두 시점의 상태를 비교해 보낼 메시지를 만든다. 알릴 게 없으면 null.
     */
    static String describeChange(String name, int port, ContainerSnapshot before, ContainerSnapshot now) {
        String server = "**" + name + "**";
        if (now.restartCount() > before.restartCount()) {
            return "⚠️ " + server + " 서버가 비정상 종료되어 자동으로 다시 켜고 있습니다.";
        }
        if (before.status() != ServerStatus.RUNNING && now.status() == ServerStatus.RUNNING) {
            return "🟢 " + server + " 서버가 켜졌습니다. (포트 " + port + ")";
        }
        if (before.isUp() && !now.isUp()) {
            if (now.exitCode() == null || NORMAL_EXIT_CODES.contains(now.exitCode())) {
                return "⚪ " + server + " 서버가 꺼졌습니다.";
            }
            return "⚠️ " + server + " 서버가 비정상 종료됐습니다. (종료 코드 " + now.exitCode() + ")";
        }
        return null;
    }

    private void send(JDA jda, String channelId, String message) {
        GuildMessageChannel channel = jda.getChannelById(GuildMessageChannel.class, channelId);
        if (channel == null) {
            log.warn("알림 채널 {}을(를) 찾을 수 없습니다. 봇이 그 서버에서 나갔거나 채널이 지워졌습니다.", channelId);
            return;
        }
        channel.sendMessage(message).queue(null, error -> log.warn("디스코드 알림을 보내지 못했습니다.", error));
    }
}
