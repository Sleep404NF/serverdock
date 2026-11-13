package io.github.sleep404nf.serverdock.user;

import io.github.sleep404nf.serverdock.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 가입, 로그인, 토큰 검사
 */
class AuthApiTest extends IntegrationTestSupport {

    @Test
    void 가입하고_로그인하면_내_정보를_볼_수_있다() throws Exception {
        String username = uniqueName("signup");
        postAs(null, "/api/auth/signup", """
                {"username": "%s", "password": "%s"}
                """.formatted(username, PASSWORD))
                .andExpect(status().isCreated());

        String token = login(username, PASSWORD);

        getAs(token, "/api/auth/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.systemRole").value("USER"));
    }

    @Test
    void 아이디가_짧으면_필드_오류와_함께_400() throws Exception {
        postAs(null, "/api/auth/signup", """
                {"username": "abc", "password": "%s"}
                """.formatted(PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").value("아이디는 6~20자여야 합니다."));
    }

    @Test
    void 이미_있는_아이디로_가입하면_409() throws Exception {
        String username = uniqueName("dup");
        String body = """
                {"username": "%s", "password": "%s"}
                """.formatted(username, PASSWORD);
        postAs(null, "/api/auth/signup", body).andExpect(status().isCreated());

        postAs(null, "/api/auth/signup", body).andExpect(status().isConflict());
    }

    @Test
    void 비밀번호가_틀리면_401() throws Exception {
        postAs(null, "/api/auth/login", """
                {"username": "admin", "password": "wrong-password"}
                """)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 토큰_없이_API를_부르면_401() throws Exception {
        getAs(null, "/api/servers").andExpect(status().isUnauthorized());
    }

    @Test
    void 잘못된_토큰이면_401() throws Exception {
        getAs("not-a-real-token", "/api/servers").andExpect(status().isUnauthorized());
    }
}
