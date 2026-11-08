-- 사용자 계정
CREATE TABLE users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    discord_user_id VARCHAR(32)  NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_discord_user_id UNIQUE (discord_user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 마인크래프트 서버 (서버 하나 = Docker 컨테이너 하나)
CREATE TABLE servers (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100) NOT NULL,
    owner_id       BIGINT       NOT NULL,
    game_version   VARCHAR(32)  NOT NULL,
    loader         VARCHAR(20)  NOT NULL,
    loader_version VARCHAR(32)  NULL,
    memory_mb      INT          NOT NULL,
    port           INT          NOT NULL,
    container_id   VARCHAR(64)  NULL,
    modpack_source VARCHAR(20)  NULL,
    modpack_ref    VARCHAR(255) NULL,
    auto_restart   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_servers_port UNIQUE (port),
    CONSTRAINT fk_servers_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_servers_loader CHECK (loader IN ('VANILLA', 'FABRIC', 'FORGE', 'NEOFORGE')),
    CONSTRAINT ck_servers_modpack_source CHECK (modpack_source IS NULL OR modpack_source IN ('MODRINTH', 'CURSEFORGE')),
    CONSTRAINT ck_servers_memory CHECK (memory_mb BETWEEN 512 AND 65536),
    CONSTRAINT ck_servers_port CHECK (port BETWEEN 1024 AND 65535)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 서버별 멤버와 역할
CREATE TABLE server_members (
    server_id BIGINT      NOT NULL,
    user_id   BIGINT      NOT NULL,
    role      VARCHAR(10) NOT NULL,
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (server_id, user_id),
    CONSTRAINT fk_server_members_server FOREIGN KEY (server_id) REFERENCES servers (id) ON DELETE CASCADE,
    CONSTRAINT fk_server_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_server_members_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    INDEX idx_server_members_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
