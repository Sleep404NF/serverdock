-- 서버 휴지통: deleted_at이 있으면 휴지통에 있는 서버다.
-- 보관 기간 동안 이름과 포트는 그대로 잡아두고, 기간이 지나면 예약 작업이 행을 완전히 지운다.
ALTER TABLE servers
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER auto_restart,
    ADD INDEX idx_servers_deleted_at (deleted_at);
