package io.github.sleep404nf.serverdock.discord;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 serverdock.discord 설정. 값은 저장소 최상위 .env 파일이나 환경변수로 넣는다.
 *
 * @param botToken     봇 토큰. 비어 있으면 봇을 켜지 않는다.
 * @param clientId     디스코드 앱의 Client ID. 계정 연결(OAuth2)에 쓴다.
 * @param clientSecret 디스코드 앱의 Client Secret. 계정 연결(OAuth2)에 쓴다.
 * @param redirectUri  계정 연결 후 디스코드가 돌려보낼 주소. 디스코드 개발자 포털에도 똑같이 등록해야 한다.
 * @param webBaseUrl   웹 대시보드 주소. 연결이 끝나면 이리로 돌려보내고, 봇 안내 메시지에도 쓴다.
 */
@ConfigurationProperties(prefix = "serverdock.discord")
public record DiscordProperties(
        String botToken,
        String clientId,
        String clientSecret,
        String redirectUri,
        String webBaseUrl
) {

    public boolean botEnabled() {
        return hasText(botToken);
    }

    public boolean oauthEnabled() {
        return hasText(clientId) && hasText(clientSecret);
    }

    public String accountPageUrl() {
        return webBaseUrl + "/#/account";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
