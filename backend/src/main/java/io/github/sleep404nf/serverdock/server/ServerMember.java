package io.github.sleep404nf.serverdock.server;

import io.github.sleep404nf.serverdock.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "server_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServerMember {

    @EmbeddedId
    private ServerMemberId id;

    @MapsId("serverId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "server_id")
    private Server server;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private ServerRole role;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    private ServerMember(Server server, User user, ServerRole role) {
        this.id = new ServerMemberId(server.getId(), user.getId());
        this.server = server;
        this.user = user;
        this.role = role;
    }

    public static ServerMember create(Server server, User user, ServerRole role) {
        return new ServerMember(server, user, role);
    }

    public void changeRole(ServerRole role) {
        this.role = role;
    }

    public boolean hasRoleAtLeast(ServerRole required) {
        return role.isAtLeast(required);
    }
}
