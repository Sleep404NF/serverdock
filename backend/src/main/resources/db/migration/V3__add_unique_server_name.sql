-- 서버 이름을 고유하게 한다.
-- 콜레이션이 utf8mb4_unicode_ci라서 대소문자를 구분하지 않고 비교한다 (Survival = survival).
ALTER TABLE servers
    ADD CONSTRAINT uk_servers_name UNIQUE (name);