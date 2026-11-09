package io.github.sleep404nf.serverdock.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 1분마다 보관 기간이 지난 휴지통 서버를 완전히 지운다.
 */
@Slf4j
@Component
@EnableScheduling
@EnableConfigurationProperties(TrashProperties.class)
@RequiredArgsConstructor
public class ServerTrashScheduler {

    private final ServerTrashService serverTrashService;

    @Scheduled(fixedDelay = 1, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void purgeExpired() {
        int purged = serverTrashService.purgeExpired();
        if (purged > 0) {
            log.info("휴지통에서 서버 {}개를 완전히 지웠습니다.", purged);
        }
    }
}
