package io.github.sleep404nf.serverdock.discord;

import io.github.sleep404nf.serverdock.common.BusinessException;
import io.github.sleep404nf.serverdock.user.User;
import io.github.sleep404nf.serverdock.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ServerDock 계정과 디스코드 계정 연결 (디스코드 OAuth2, identify 권한만 받는다).
 *
 * 1. 웹에서 연결을 시작하면 state(일회용 값)를 만들어 누가 시작했는지 기억하고, 디스코드 승인 화면 주소를 준다.
 * 2. 사용자가 승인하면 디스코드가 code와 state를 붙여 callback으로 돌려보낸다.
 * 3. state로 사용자를 찾고, code를 토큰으로 바꿔 디스코드 사용자 id를 읽어 계정에 저장한다.
 *
 * state는 메모리에만 두고 10분이 지나면 버린다. 백엔드를 재시작하면 진행 중이던 연결은 처음부터 다시 하면 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordAccountService {

    private static final String AUTHORIZE_URL = "https://discord.com/oauth2/authorize";
    private static final String TOKEN_URL = "https://discord.com/api/oauth2/token";
    private static final String ME_URL = "https://discord.com/api/users/@me";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DiscordProperties properties;
    private final UserRepository userRepository;
    private final RestClient restClient = RestClient.create();
    private final Map<String, PendingLink> pendingLinks = new ConcurrentHashMap<>();

    private record PendingLink(Long userId, Instant expiresAt) {
    }

    /**
     * 연결 시작. 브라우저를 보낼 디스코드 승인 화면 주소를 돌려준다.
     */
    public String startLink(Long userId) {
        if (!properties.oauthEnabled()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "디스코드 연동이 설정되지 않았습니다. 관리자에게 문의해주세요.");
        }
        Instant now = Instant.now();
        pendingLinks.values().removeIf(link -> link.expiresAt().isBefore(now));

        String state = randomState();
        pendingLinks.put(state, new PendingLink(userId, now.plus(STATE_TTL)));

        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.clientId())
                .queryParam("scope", "identify")
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("state", state)
                .encode()
                .build()
                .toUriString();
    }

    /**
     * 디스코드가 돌려보낸 요청 처리. 성공이든 실패든 웹 대시보드의 계정 화면 주소를 돌려준다.
     */
    @Transactional
    public String completeLink(String code, String state, String error) {
        PendingLink pending = state == null ? null : pendingLinks.remove(state);
        if (pending == null || pending.expiresAt().isBefore(Instant.now())) {
            return resultUrl("expired");
        }
        if (error != null || code == null) {
            // 사용자가 디스코드 화면에서 "취소"를 누른 경우
            return resultUrl("cancelled");
        }

        String discordUserId;
        String discordUsername;
        try {
            String accessToken = exchangeCode(code);
            Map<String, Object> me = fetchMe(accessToken);
            discordUserId = String.valueOf(me.get("id"));
            Object globalName = me.get("global_name");
            discordUsername = globalName != null ? String.valueOf(globalName) : String.valueOf(me.get("username"));
        } catch (RestClientException | IllegalStateException e) {
            log.warn("디스코드 계정 정보를 받지 못했습니다.", e);
            return resultUrl("failed");
        }

        User owner = userRepository.findByDiscordUserId(discordUserId).orElse(null);
        if (owner != null && !owner.getId().equals(pending.userId())) {
            return resultUrl("taken");
        }

        User user = userRepository.findById(pending.userId()).orElse(null);
        if (user == null) {
            return resultUrl("failed");
        }
        user.linkDiscord(discordUserId, discordUsername);
        log.info("디스코드 계정을 연결했습니다: 사용자 {} ↔ 디스코드 {}", user.getUsername(), discordUsername);
        return resultUrl("linked");
    }

    @Transactional
    public void unlink(Long userId) {
        userRepository.findById(userId).ifPresent(User::unlinkDiscord);
    }

    private String exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", properties.redirectUri());

        Map<String, Object> token = restClient.post()
                .uri(TOKEN_URL)
                .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (token == null || token.get("access_token") == null) {
            throw new IllegalStateException("디스코드가 토큰을 주지 않았습니다.");
        }
        return String.valueOf(token.get("access_token"));
    }

    private Map<String, Object> fetchMe(String accessToken) {
        Map<String, Object> me = restClient.get()
                .uri(ME_URL)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (me == null || me.get("id") == null) {
            throw new IllegalStateException("디스코드 사용자 정보가 비어 있습니다.");
        }
        return me;
    }

    private String resultUrl(String result) {
        return properties.accountPageUrl() + "?discord=" + result;
    }

    private static String randomState() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
