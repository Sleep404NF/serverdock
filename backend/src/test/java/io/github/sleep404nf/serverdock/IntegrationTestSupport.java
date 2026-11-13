package io.github.sleep404nf.serverdock;

import com.jayway.jsonpath.JsonPath;
import io.github.sleep404nf.serverdock.docker.MinecraftContainerManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 통합 테스트의 공통 준비. 실제 Spring 앱을 띄우고 MockMvc로 HTTP 요청을 보낸다.
 * - DB: Testcontainers MariaDB (테스트 실행마다 새로 만든다)
 * - Docker: 가짜(MockitoBean)로 바꿔서 마인크래프트 서버를 실제로 띄우지 않는다
 * - 계정: 앱이 시작할 때 만드는 admin/admin(시스템 관리자)을 쓴다
 * 테스트끼리 DB를 같이 쓰므로 아이디와 서버 이름은 uniqueName으로 겹치지 않게 만든다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    protected static final String PASSWORD = "password1234";
    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    protected MinecraftContainerManager containerManager;

    protected MockMvc mvc;

    @BeforeEach
    void setUpMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    /** 영문, 숫자, 밑줄로 된 겹치지 않는 이름. 아이디 규칙(6~20자)에도 맞는다. */
    protected static String uniqueName(String prefix) {
        return prefix + "_" + COUNTER.incrementAndGet();
    }

    // ---- 요청 도우미 ----

    protected ResultActions call(MockHttpServletRequestBuilder request, String token) throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request);
    }

    protected ResultActions getAs(String token, String url) throws Exception {
        return call(get(url), token);
    }

    protected ResultActions postAs(String token, String url, String json) throws Exception {
        MockHttpServletRequestBuilder request = post(url);
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return call(request, token);
    }

    protected ResultActions patchAs(String token, String url, String json) throws Exception {
        return call(patch(url).contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    protected ResultActions deleteAs(String token, String url) throws Exception {
        return call(delete(url), token);
    }

    protected static <T> T read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    // ---- 자주 쓰는 준비 과정 ----

    protected String login(String username, String password) throws Exception {
        ResultActions result = postAs(null, "/api/auth/login",
                """
                {"username": "%s", "password": "%s"}
                """.formatted(username, password))
                .andExpect(status().isOk());
        return read(result, "$.accessToken");
    }

    protected String adminToken() throws Exception {
        return login("admin", "admin");
    }

    /** 새 일반 사용자를 가입시키고 토큰을 돌려준다. */
    protected String newUser() throws Exception {
        String username = uniqueName("user");
        postAs(null, "/api/auth/signup",
                """
                {"username": "%s", "password": "%s"}
                """.formatted(username, PASSWORD))
                .andExpect(status().isCreated());
        return login(username, PASSWORD);
    }

    protected long userId(String token) throws Exception {
        return ((Number) read(getAs(token, "/api/auth/me"), "$.id")).longValue();
    }

    /** 관리자가 서버를 만든다. 관리자가 OWNER가 된다. */
    protected long createServer(String adminToken) throws Exception {
        ResultActions result = postAs(adminToken, "/api/servers",
                """
                {"name": "%s", "gameVersion": "1.21.4", "loader": "VANILLA"}
                """.formatted(uniqueName("server")))
                .andExpect(status().isCreated());
        return ((Number) read(result, "$.id")).longValue();
    }

    protected String createInvite(String token, long serverId, String role, Integer maxUses) throws Exception {
        String maxUsesJson = maxUses == null ? "null" : maxUses.toString();
        ResultActions result = postAs(token, "/api/servers/" + serverId + "/invites",
                """
                {"role": "%s", "maxUses": %s}
                """.formatted(role, maxUsesJson))
                .andExpect(status().isCreated());
        return read(result, "$.code");
    }

    /** 초대 링크로 서버에 들어온 새 사용자. 역할은 초대 링크의 역할을 따른다. */
    protected String joinedUser(String ownerToken, long serverId, String role) throws Exception {
        String token = newUser();
        String code = createInvite(ownerToken, serverId, role, 1);
        postAs(token, "/api/invites/" + code + "/accept", null).andExpect(status().isOk());
        return token;
    }
}
