package io.github.sleep404nf.serverdock.server;

import io.github.sleep404nf.serverdock.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 역할 변경, 내보내기, 나가기 규칙
 */
class MemberRulesTest extends IntegrationTestSupport {

    @Test
    void 자기보다_낮은_역할만_내보낼_수_있다() throws Exception {
        String owner = adminToken();
        long serverId = createServer(owner);
        String admin = joinedUser(owner, serverId, "ADMIN");
        String otherAdmin = joinedUser(owner, serverId, "ADMIN");
        String member = joinedUser(owner, serverId, "MEMBER");
        String otherMember = joinedUser(owner, serverId, "MEMBER");
        String members = "/api/servers/" + serverId + "/members/";

        // MEMBER는 아무도 못 내보낸다
        deleteAs(member, members + userId(otherMember)).andExpect(status().isForbidden());
        // ADMIN은 같은 ADMIN을 못 내보낸다
        deleteAs(admin, members + userId(otherAdmin)).andExpect(status().isForbidden());
        // ADMIN은 MEMBER를 내보낼 수 있다
        deleteAs(admin, members + userId(member)).andExpect(status().isNoContent());
        // 내보내진 사람에게는 서버가 안 보인다
        getAs(member, "/api/servers/" + serverId).andExpect(status().isNotFound());
    }

    @Test
    void OWNER는_나갈_수_없고_다른_멤버는_나갈_수_있다() throws Exception {
        String owner = adminToken();
        long serverId = createServer(owner);
        String member = joinedUser(owner, serverId, "MEMBER");
        String members = "/api/servers/" + serverId + "/members/";

        deleteAs(owner, members + userId(owner)).andExpect(status().isBadRequest());
        deleteAs(member, members + userId(member)).andExpect(status().isNoContent());
        getAs(member, "/api/servers/" + serverId).andExpect(status().isNotFound());
    }

    @Test
    void 역할은_OWNER만_ADMIN과_MEMBER_사이로_바꿀_수_있다() throws Exception {
        String owner = adminToken();
        long serverId = createServer(owner);
        String admin = joinedUser(owner, serverId, "ADMIN");
        String member = joinedUser(owner, serverId, "MEMBER");
        String memberUrl = "/api/servers/" + serverId + "/members/" + userId(member);

        patchAs(admin, memberUrl, """
                {"role": "ADMIN"}
                """).andExpect(status().isForbidden());
        patchAs(owner, memberUrl, """
                {"role": "OWNER"}
                """).andExpect(status().isBadRequest());
        patchAs(owner, memberUrl, """
                {"role": "ADMIN"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
