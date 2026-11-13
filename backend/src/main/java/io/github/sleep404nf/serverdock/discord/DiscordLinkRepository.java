package io.github.sleep404nf.serverdock.discord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DiscordLinkRepository extends JpaRepository<DiscordLink, Long> {

    Optional<DiscordLink> findByServerId(Long serverId);

    /** 알림을 보낼 대상. 휴지통에 있는 서버는 뺀다. */
    @Query("select l from DiscordLink l join fetch l.server s where s.deletedAt is null")
    List<DiscordLink> findAllActiveWithServer();
}
