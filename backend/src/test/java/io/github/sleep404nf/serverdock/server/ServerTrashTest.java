package io.github.sleep404nf.serverdock.server;

import io.github.sleep404nf.serverdock.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 서버 삭제(휴지통), 복구, 보관 기간이 지난 서버 완전 삭제
 */
class ServerTrashTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ServerTrashService serverTrashService;

    @Test
    void 지운_서버는_숨겨지고_이름은_잡혀_있다가_복구된다() throws Exception {
        String owner = adminToken();
        long serverId = createServer(owner);
        String member = joinedUser(owner, serverId, "MEMBER");
        String name = read(getAs(owner, "/api/servers/" + serverId), "$.name");
        int id = (int) serverId;

        deleteAs(owner, "/api/servers/" + serverId).andExpect(status().isNoContent());
        verify(containerManager).stop(serverId);

        // 멤버 모두에게 없는 서버처럼 보인다
        getAs(owner, "/api/servers/" + serverId).andExpect(status().isNotFound());
        getAs(member, "/api/servers/" + serverId).andExpect(status().isNotFound());
        getAs(owner, "/api/servers").andExpect(jsonPath("$[*].id", not(hasItem(id))));

        // OWNER의 휴지통에만 보인다
        getAs(owner, "/api/servers/trash").andExpect(jsonPath("$[*].id", hasItem(id)));
        getAs(member, "/api/servers/trash").andExpect(jsonPath("$[*].id", not(hasItem(id))));

        // 보관 중에는 같은 이름으로 새 서버를 못 만든다
        postAs(owner, "/api/servers", """
                {"name": "%s", "gameVersion": "1.21.4", "loader": "VANILLA"}
                """.formatted(name))
                .andExpect(status().isConflict());

        // OWNER만 복구할 수 있고, 복구하면 다시 보인다
        postAs(member, "/api/servers/" + serverId + "/restore", null).andExpect(status().isForbidden());
        postAs(owner, "/api/servers/" + serverId + "/restore", null).andExpect(status().isOk());
        getAs(member, "/api/servers/" + serverId).andExpect(status().isOk());
    }

    @Test
    void 휴지통에_없는_서버를_복구하면_400() throws Exception {
        String owner = adminToken();
        long serverId = createServer(owner);

        postAs(owner, "/api/servers/" + serverId + "/restore", null).andExpect(status().isBadRequest());
    }

    @Test
    void 보관_기간이_지나면_컨테이너와_DB_기록까지_지운다() throws Exception {
        String owner = adminToken();
        long serverId = createServer(owner);
        deleteAs(owner, "/api/servers/" + serverId).andExpect(status().isNoContent());
        // 지운 시각을 보관 기간(2일)보다 전으로 돌린다
        jdbcTemplate.update("UPDATE servers SET deleted_at = NOW() - INTERVAL 3 DAY WHERE id = ?", serverId);

        int purged = serverTrashService.purgeExpired();

        assertThat(purged).isGreaterThanOrEqualTo(1);
        verify(containerManager).removeServer(serverId);
        Integer servers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM servers WHERE id = ?", Integer.class, serverId);
        Integer members = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM server_members WHERE server_id = ?", Integer.class, serverId);
        assertThat(servers).isZero();
        assertThat(members).isZero();
    }

    @Test
    void 보관_기간이_안_지난_서버는_남겨둔다() throws Exception {
        String owner = adminToken();
        long serverId = createServer(owner);
        deleteAs(owner, "/api/servers/" + serverId).andExpect(status().isNoContent());

        serverTrashService.purgeExpired();

        Integer servers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM servers WHERE id = ?", Integer.class, serverId);
        assertThat(servers).isOne();
    }
}
