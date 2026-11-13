package io.github.sleep404nf.serverdock.invite;

import io.github.sleep404nf.serverdock.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 초대 링크 만들기, 수락, 만료, 횟수 제한, 폐기
 */
class InviteFlowTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 초대를_수락하면_초대_링크의_역할로_멤버가_된다() throws Exception {
        String owner = adminToken();
        long serverId = createServer(owner);
        String code = createInvite(owner, serverId, "MEMBER", null);
        String user = newUser();

        postAs(user, "/api/invites/" + code + "/accept", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serverId))
                .andExpect(jsonPath("$.myRole").value("MEMBER"));
    }

    @Test
    void 사용_횟수가_다_차면_410_이미_멤버면_409() throws Exception {
        String owner = adminToken();
        long serverId = createServer(owner);
        String code = createInvite(owner, serverId, "MEMBER", 1);
        String first = newUser();
        String second = newUser();
        String acceptUrl = "/api/invites/" + code + "/accept";

        postAs(first, acceptUrl, null).andExpect(status().isOk());
        postAs(first, acceptUrl, null).andExpect(status().isConflict());
        postAs(second, acceptUrl, null).andExpect(status().isGone());

        getAs(owner, "/api/servers/" + serverId + "/invites")
                .andExpect(jsonPath("$[0].usedCount").value(1))
                .andExpect(jsonPath("$[0].usable").value(false));
    }

    @Test
    void 만료된_초대는_410() throws Exception {
        String owner = adminToken();
        long serverId = createServer(owner);
        String code = createInvite(owner, serverId, "MEMBER", null);
        // 만료 시간을 지나간 시각으로 바꾼다
        jdbcTemplate.update("UPDATE invites SET expires_at = NOW() - INTERVAL 1 MINUTE WHERE code = ?", code);

        postAs(newUser(), "/api/invites/" + code + "/accept", null)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.detail").value("만료된 초대 링크입니다."));
    }

    @Test
    void 폐기한_초대는_404() throws Exception {
        String owner = adminToken();
        long serverId = createServer(owner);
        String code = createInvite(owner, serverId, "MEMBER", null);
        Number inviteId = read(getAs(owner, "/api/servers/" + serverId + "/invites"), "$[0].id");

        deleteAs(owner, "/api/servers/" + serverId + "/invites/" + inviteId).andExpect(status().isNoContent());

        postAs(newUser(), "/api/invites/" + code + "/accept", null).andExpect(status().isNotFound());
    }

    @Test
    void ADMIN_초대는_OWNER만_만들_수_있고_MEMBER는_초대를_못_만든다() throws Exception {
        String owner = adminToken();
        long serverId = createServer(owner);
        String admin = joinedUser(owner, serverId, "ADMIN");
        String member = joinedUser(owner, serverId, "MEMBER");
        String invitesUrl = "/api/servers/" + serverId + "/invites";

        postAs(admin, invitesUrl, """
                {"role": "ADMIN"}
                """).andExpect(status().isForbidden());
        postAs(admin, invitesUrl, """
                {"role": "MEMBER"}
                """).andExpect(status().isCreated());
        postAs(member, invitesUrl, """
                {"role": "MEMBER"}
                """).andExpect(status().isForbidden());
    }
}
