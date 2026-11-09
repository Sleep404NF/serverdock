package io.github.sleep404nf.serverdock.server;

import io.github.sleep404nf.serverdock.common.BaseTimeEntity;
import io.github.sleep404nf.serverdock.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "servers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Server extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "game_version", nullable = false, length = 32)
    private String gameVersion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ServerLoader loader;

    @Column(name = "loader_version", length = 32)
    private String loaderVersion;

    @Column(name = "memory_mb", nullable = false)
    private int memoryMb;

    @Column(nullable = false, unique = true)
    private int port;

    @Column(name = "container_id", length = 64)
    private String containerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "modpack_source", length = 20)
    private ModpackSource modpackSource;

    @Column(name = "modpack_ref")
    private String modpackRef;

    @Column(name = "auto_restart", nullable = false, columnDefinition = "tinyint(1)")
    private boolean autoRestart;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Server(String name, User owner, String gameVersion, ServerLoader loader,
                   String loaderVersion, int memoryMb, int port) {
        this.name = name;
        this.owner = owner;
        this.gameVersion = gameVersion;
        this.loader = loader;
        this.loaderVersion = loaderVersion;
        this.memoryMb = memoryMb;
        this.port = port;
        this.autoRestart = true;
    }

    public static Server create(String name, User owner, String gameVersion, ServerLoader loader,
                                String loaderVersion, int memoryMb, int port) {
        return new Server(name, owner, gameVersion, loader, loaderVersion, memoryMb, port);
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeMemory(int memoryMb) {
        this.memoryMb = memoryMb;
    }

    public void changeAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
    }

    public void useModpack(ModpackSource source, String ref) {
        this.modpackSource = source;
        this.modpackRef = ref;
    }

    public void clearModpack() {
        this.modpackSource = null;
        this.modpackRef = null;
    }

    public void attachContainer(String containerId) {
        this.containerId = containerId;
    }

    public void detachContainer() {
        this.containerId = null;
    }

    public boolean isInTrash() {
        return deletedAt != null;
    }

    public void moveToTrash(LocalDateTime now) {
        this.deletedAt = now;
    }

    public void restore() {
        this.deletedAt = null;
    }
}
