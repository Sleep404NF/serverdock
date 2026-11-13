package io.github.sleep404nf.serverdock.discord;

import io.github.sleep404nf.serverdock.server.Server;
import io.github.sleep404nf.serverdock.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 서버 상태 알림을 보낼 디스코드 채널. 서버 하나에 채널 하나.
 */
@Getter
@Entity
@Table(name = "discord_links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscordLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @Column(name = "guild_id", nullable = false, length = 32)
    private String guildId;

    @Column(name = "channel_id", nullable = false, length = 32)
    private String channelId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private DiscordLink(Server server, String guildId, String channelId, User createdBy) {
        this.server = server;
        this.guildId = guildId;
        this.channelId = channelId;
        this.createdBy = createdBy;
    }

    public static DiscordLink create(Server server, String guildId, String channelId, User createdBy) {
        return new DiscordLink(server, guildId, channelId, createdBy);
    }

    public void moveTo(String guildId, String channelId, User changedBy) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.createdBy = changedBy;
    }
}
