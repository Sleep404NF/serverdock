-- 서버 초대 링크
CREATE TABLE invites (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    server_id  BIGINT      NOT NULL,
    code       VARCHAR(32) NOT NULL,
    role       VARCHAR(10) NOT NULL,
    created_by BIGINT      NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    max_uses   INT         NULL,
    used_count INT         NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_invites_code UNIQUE (code),
    CONSTRAINT fk_invites_server FOREIGN KEY (server_id) REFERENCES servers (id) ON DELETE CASCADE,
    CONSTRAINT fk_invites_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_invites_role CHECK (role IN ('ADMIN', 'MEMBER')),
    CONSTRAINT ck_invites_max_uses CHECK (max_uses IS NULL OR max_uses >= 1),
    CONSTRAINT ck_invites_used_count CHECK (used_count >= 0 AND (max_uses IS NULL OR used_count <= max_uses)),
    INDEX idx_invites_server (server_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
