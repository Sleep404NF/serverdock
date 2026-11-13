package io.github.sleep404nf.serverdock.discord;

import io.github.sleep404nf.serverdock.common.BusinessException;
import io.github.sleep404nf.serverdock.server.ServerAccessChecker;
import io.github.sleep404nf.serverdock.server.ServerMember;
import io.github.sleep404nf.serverdock.server.ServerRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서버 상태 알림 채널 지정과 해제. ADMIN 이상만 가능하다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DiscordChannelService {

    private final ServerAccessChecker serverAccessChecker;
    private final DiscordLinkRepository discordLinkRepository;

    /** 이 채널로 알림을 보낸다. 이미 다른 채널이 지정돼 있으면 옮긴다. */
    public void link(Long userId, Long serverId, String guildId, String channelId) {
        ServerMember me = serverAccessChecker.require(serverId, userId, ServerRole.ADMIN);

        discordLinkRepository.findByServerId(serverId).ifPresentOrElse(
                link -> link.moveTo(guildId, channelId, me.getUser()),
                () -> discordLinkRepository.save(DiscordLink.create(me.getServer(), guildId, channelId, me.getUser()))
        );
    }

    public void unlink(Long userId, Long serverId) {
        serverAccessChecker.require(serverId, userId, ServerRole.ADMIN);

        DiscordLink link = discordLinkRepository.findByServerId(serverId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "알림 채널이 지정되어 있지 않습니다."));
        discordLinkRepository.delete(link);
    }
}
