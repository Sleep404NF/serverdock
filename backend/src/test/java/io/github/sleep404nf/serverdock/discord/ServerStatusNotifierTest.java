package io.github.sleep404nf.serverdock.discord;

import io.github.sleep404nf.serverdock.docker.ContainerSnapshot;
import io.github.sleep404nf.serverdock.docker.ServerStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상태 변화를 어떤 알림으로 바꾸는지
 */
class ServerStatusNotifierTest {

    private static ContainerSnapshot snapshot(ServerStatus status, Long exitCode, int restartCount) {
        return new ContainerSnapshot(status, exitCode, restartCount);
    }

    private static String describe(ContainerSnapshot before, ContainerSnapshot now) {
        return ServerStatusNotifier.describeChange("야생", 25565, before, now);
    }

    @Test
    void 켜지면_켜짐_알림() {
        assertThat(describe(snapshot(ServerStatus.STARTING, 0L, 0), snapshot(ServerStatus.RUNNING, 0L, 0)))
                .contains("켜졌습니다").contains("25565");
    }

    @Test
    void 정상_종료면_꺼짐_알림() {
        assertThat(describe(snapshot(ServerStatus.RUNNING, 0L, 0), snapshot(ServerStatus.STOPPED, 0L, 0)))
                .contains("꺼졌습니다");
        assertThat(describe(snapshot(ServerStatus.RUNNING, 0L, 0), snapshot(ServerStatus.STOPPED, 143L, 0)))
                .contains("꺼졌습니다");
    }

    @Test
    void 종료_코드가_이상하면_비정상_종료_알림() {
        assertThat(describe(snapshot(ServerStatus.RUNNING, 0L, 0), snapshot(ServerStatus.STOPPED, 137L, 0)))
                .contains("비정상 종료").contains("137");
    }

    @Test
    void 자동_재시작_횟수가_늘면_비정상_종료_후_재시작_알림() {
        assertThat(describe(snapshot(ServerStatus.RUNNING, 0L, 0), snapshot(ServerStatus.STARTING, 137L, 1)))
                .contains("자동으로 다시 켜고");
    }

    @Test
    void 바뀐_게_없으면_알리지_않는다() {
        assertThat(describe(snapshot(ServerStatus.RUNNING, 0L, 0), snapshot(ServerStatus.RUNNING, 0L, 0))).isNull();
        assertThat(describe(snapshot(ServerStatus.STOPPED, 0L, 0), snapshot(ServerStatus.STARTING, 0L, 0))).isNull();
    }
}
