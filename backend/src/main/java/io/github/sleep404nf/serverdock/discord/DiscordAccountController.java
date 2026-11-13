package io.github.sleep404nf.serverdock.discord;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/discord")
@RequiredArgsConstructor
public class DiscordAccountController {

    private final DiscordAccountService discordAccountService;

    /** 연결 시작. 응답의 url로 브라우저를 보내면 디스코드 승인 화면이 나온다. */
    @PostMapping("/link")
    public Map<String, String> startLink(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("url", discordAccountService.startLink(userId(jwt)));
    }

    /** 디스코드가 브라우저를 돌려보내는 곳. 처리 후 웹 대시보드 계정 화면으로 다시 보낸다. */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error) {
        String redirect = discordAccountService.completeLink(code, state, error);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(redirect).toString())
                .build();
    }

    @DeleteMapping("/link")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@AuthenticationPrincipal Jwt jwt) {
        discordAccountService.unlink(userId(jwt));
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
