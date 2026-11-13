-- 디스코드 연동
-- users.discord_user_id는 V1에 이미 있다. 화면에 보여줄 디스코드 이름만 더한다.
ALTER TABLE users
    ADD COLUMN discord_username VARCHAR(64) NULL AFTER discord_user_id;

-- 서버 상태 알림을 보낼 디스코드 채널. 서버 하나에 채널 하나.
CREATE TABLE discord_links (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    server_id  BIGINT      NOT NULL,
    guild_id   VARCHAR(32) NOT NULL,
    channel_id VARCHAR(32) NOT NULL,
    created_by BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_discord_links_server UNIQUE (server_id),
    CONSTRAINT fk_discord_links_server FOREIGN KEY (server_id) REFERENCES servers (id) ON DELETE CASCADE,
    CONSTRAINT fk_discord_links_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
