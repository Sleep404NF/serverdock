package io.github.sleep404nf.serverdock.server;

import io.github.sleep404nf.serverdock.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 서버 생성 권한과 서버별 권한 검사(외부인 404, 역할 부족 403)
 */
class ServerPermissionTest extends IntegrationTestSupport {

    @Test
    void 서버는_시스템_관리자만_만들_수_있다() throws Exception {
        String user = newUser();

        postAs(user, "/api/servers", """
                {"name": "%s", "gameVersion": "1.21.4", "loader": "VANILLA"}
                """.formatted(uniqueName("server")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 만든_사람은_OWNER가_되고_메모리를_안_보내면_2048MB() throws Exception {
        String admin = adminToken();
        long serverId = createServer(admin);

        getAs(admin, "/api/servers/" + serverId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.memoryMb").value(2048));
    }

    @Test
    void 서버_이름은_대소문자를_가리지_않고_겹칠_수_없다() throws Exception {
        String admin = adminToken();
        String name = uniqueName("Survival");
        postAs(admin, "/api/servers", """
                {"name": "%s", "gameVersion": "1.21.4", "loader": "VANILLA"}
                """.formatted(name))
                .andExpect(status().isCreated());

        postAs(admin, "/api/servers", """
                {"name": "%s", "gameVersion": "1.21.4", "loader": "VANILLA"}
                """.formatted(name.toLowerCase()))
                .andExpect(status().isConflict());
    }

    @Test
    void 외부인에게는_서버가_없는_것처럼_404() throws Exception {
        long serverId = createServer(adminToken());
        String outsider = newUser();

        getAs(outsider, "/api/servers/" + serverId).andExpect(status().isNotFound());
        getAs(outsider, "/api/servers/" + serverId + "/members").andExpect(status().isNotFound());
    }

    @Test
    void 설정_변경은_OWNER만_가능하다() throws Exception {
        String owner = adminToken();
        long serverId = createServer(owner);
        String admin = joinedUser(owner, serverId, "ADMIN");
        String member = joinedUser(owner, serverId, "MEMBER");
        String body = """
                {"memoryMb": 4096}
                """;

        patchAs(member, "/api/servers/" + serverId, body).andExpect(status().isForbidden());
        patchAs(admin, "/api/servers/" + serverId, body).andExpect(status().isForbidden());
        patchAs(owner, "/api/servers/" + serverId, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memoryMb").value(4096));
    }

    @Test
    void 콘솔_명령어는_ADMIN_이상만_보낼_수_있다() throws Exception {
        String owner = adminToken();
        long serverId = createServer(owner);
        String member = joinedUser(owner, serverId, "MEMBER");

        postAs(member, "/api/servers/" + serverId + "/console/commands", """
                {"command": "list"}
                """)
                .andExpect(status().isForbidden());
    }
}
