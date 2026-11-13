package io.github.sleep404nf.serverdock.docker;

/**
 * 컨테이너를 만들 때 필요한 서버 정보. type은 VANILLA, FABRIC, FORGE, NEOFORGE 중 하나.
 * autoRestart가 true면 서버가 죽거나 컴퓨터(Docker)가 다시 켜졌을 때 자동으로 다시 켠다.
 */
public record MinecraftServerSpec(
        long serverId,
        int port,
        String type,
        String gameVersion,
        String loaderVersion,
        int memoryMb,
        boolean autoRestart
) {
}