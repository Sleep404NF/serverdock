package io.github.sleep404nf.serverdock.docker;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 콘솔로 보내기 전에 색상 코드를 지우는지
 */
class MinecraftConsoleTest {

    @Test
    void 터미널_색상_코드를_지운다() {
        assertThat(MinecraftConsole.clean("\u001B[32m[INFO]\u001B[0m Done")).isEqualTo("[INFO] Done");
    }

    @Test
    void 마인크래프트_색상_코드를_지운다() {
        assertThat(MinecraftConsole.clean("§a초록 §l굵게§r 보통")).isEqualTo("초록 굵게 보통");
    }

    @Test
    void 일반_문장은_그대로_둔다() {
        assertThat(MinecraftConsole.clean("There are 0 of a max of 20 players online:"))
                .isEqualTo("There are 0 of a max of 20 players online:");
    }
}
