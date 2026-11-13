package io.github.sleep404nf.serverdock.discord;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;

/**
 * 디스코드 봇 연결. 백엔드가 다 뜬 뒤에 접속하고, 백엔드가 꺼질 때 같이 끊는다.
 * 토큰이 없으면 봇 없이 웹만 동작한다.
 * 추가 권한(intent)은 받지 않는다. 길드와 채널 목록(GUILDS)은 JDA가 항상 켜 두므로 따로 적지 않아도
 * 명령어 등록과 알림 보내기가 된다.
 */
@Slf4j
@Component
@EnableConfigurationProperties(DiscordProperties.class)
@RequiredArgsConstructor
public class DiscordBot {

    private final DiscordProperties properties;
    private final ServerCommandListener commandListener;
    private volatile JDA jda;

    @EventListener(ApplicationReadyEvent.class)
    public void connect() {
        if (!properties.botEnabled()) {
            log.info("디스코드 봇 토큰이 없어서 봇을 켜지 않습니다.");
            return;
        }
        try {
            jda = JDABuilder.createLight(properties.botToken(), EnumSet.noneOf(GatewayIntent.class))
                    .addEventListeners(commandListener)
                    .build();
            log.info("디스코드 봇에 접속하는 중입니다.");
        } catch (InvalidTokenException | IllegalArgumentException e) {
            log.error("디스코드 봇을 켜지 못했습니다. DISCORD_BOT_TOKEN을 확인해주세요.", e);
        }
    }

    @PreDestroy
    public void disconnect() {
        JDA current = jda;
        if (current != null) {
            current.shutdown();
        }
    }

    /** 접속이 끝난 봇. 아직 접속 중이거나 봇이 꺼져 있으면 비어 있다. */
    public Optional<JDA> connected() {
        JDA current = jda;
        return current != null && current.getStatus() == JDA.Status.CONNECTED ? Optional.of(current) : Optional.empty();
    }
}
