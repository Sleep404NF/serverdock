package io.github.sleep404nf.serverdock.user;

import io.github.sleep404nf.serverdock.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "system_role", nullable = false, length = 10)
    private SystemRole systemRole;

    @Column(name = "discord_user_id", unique = true, length = 32)
    private String discordUserId;

    @Column(name = "discord_username", length = 64)
    private String discordUsername;

    private User(String username, String passwordHash, SystemRole systemRole) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.systemRole = systemRole;
    }

    public static User create(String username, String passwordHash, SystemRole systemRole) {
        return new User(username, passwordHash, systemRole);
    }

    public boolean isAdmin() {
        return systemRole == SystemRole.ADMIN;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void linkDiscord(String discordUserId, String discordUsername) {
        this.discordUserId = discordUserId;
        this.discordUsername = discordUsername;
    }

    public void unlinkDiscord() {
        this.discordUserId = null;
        this.discordUsername = null;
    }
}
