package io.github.sleep404nf.serverdock.docker;

/**
 * 컨테이너를 만들 때 필요한 서버 정보. type은 VANILLA, FABRIC, FORGE, NEOFORGE 중 하나.
 */
public record MinecraftServerSpec(
        long serverId,
        int port,
        String type,
        String gameVersion,
        String loaderVersion,
        int memoryMb
) {
}