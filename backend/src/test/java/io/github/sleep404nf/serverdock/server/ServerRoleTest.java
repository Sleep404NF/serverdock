package io.github.sleep404nf.serverdock.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 역할 비교. 권한 검사(isAtLeast)와 내보내기 규칙(outranks)의 기반이다.
 */
class ServerRoleTest {

    @Test
    void isAtLeast는_같은_역할도_포함한다() {
        assertThat(ServerRole.OWNER.isAtLeast(ServerRole.ADMIN)).isTrue();
        assertThat(ServerRole.ADMIN.isAtLeast(ServerRole.ADMIN)).isTrue();
        assertThat(ServerRole.MEMBER.isAtLeast(ServerRole.ADMIN)).isFalse();
    }

    @Test
    void outranks는_같은_역할을_포함하지_않는다() {
        assertThat(ServerRole.OWNER.outranks(ServerRole.ADMIN)).isTrue();
        assertThat(ServerRole.ADMIN.outranks(ServerRole.MEMBER)).isTrue();
        assertThat(ServerRole.ADMIN.outranks(ServerRole.ADMIN)).isFalse();
        assertThat(ServerRole.MEMBER.outranks(ServerRole.OWNER)).isFalse();
    }
}
