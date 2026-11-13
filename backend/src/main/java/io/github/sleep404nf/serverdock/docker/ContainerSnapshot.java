package io.github.sleep404nf.serverdock.docker;

/**
 * 한 시점의 컨테이너 상태. 디스코드 알림에서 이전 상태와 비교하는 데 쓴다.
 *
 * @param status       서버 상태
 * @param exitCode     마지막으로 꺼졌을 때의 종료 코드. 0이면 정상 종료. 컨테이너가 없으면 null
 * @param restartCount Docker가 자동 재시작한 횟수. 늘어났다면 그사이 비정상 종료 후 다시 켜진 것
 */
public record ContainerSnapshot(ServerStatus status, Long exitCode, int restartCount) {

    public static ContainerSnapshot notCreated() {
        return new ContainerSnapshot(ServerStatus.NOT_CREATED, null, 0);
    }

    public boolean isUp() {
        return status == ServerStatus.STARTING || status == ServerStatus.RUNNING;
    }
}
