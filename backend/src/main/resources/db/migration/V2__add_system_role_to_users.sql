-- 시스템 역할: ADMIN만 서버를 만들 수 있다. 첫 가입자가 ADMIN이 된다.
ALTER TABLE users
    ADD COLUMN system_role VARCHAR(10) NOT NULL DEFAULT 'USER' AFTER password_hash,
    ADD CONSTRAINT ck_users_system_role CHECK (system_role IN ('ADMIN', 'USER'));
