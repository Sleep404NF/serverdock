# ServerDock

친구끼리 쓰는 마인크래프트 서버를 웹에서 만들고, 켜고, 관리하는 셀프호스팅 도구다.

> 개발 중이다. 지금은 계정, 서버 설정, 멤버와 권한, 초대 링크 API까지 동작하고, 실제 마인크래프트 서버를 띄우는 Docker 연동이 다음 단계다.

## 왜 만들었나

- 모드 서버를 열려면 로더, 모드팩, Java 버전을 일일이 맞춰야 한다.
- 서버가 한 사람의 컴퓨터에 묶여 있어서, 주인이 없으면 친구들이 서버를 켤 수 없다.
- 서버가 켜져 있는지, 누가 접속 중인지 보려면 터미널을 열어야 한다.

## 기능

| 기능 | 상태 |
| --- | --- |
| 회원가입, 로그인 (JWT) | 완료 |
| 서버 생성, 설정 변경, 삭제 | 완료 |
| 서버별 역할 (OWNER / ADMIN / MEMBER) | 완료 |
| 초대 링크 (만료 시간, 사용 횟수 제한) | 완료 |
| 멤버 관리 (역할 변경, 내보내기, 나가기) | 완료 |
| Docker로 마인크래프트 서버 실행, 실시간 콘솔 | 예정 |
| 디스코드 봇, 월드 백업 | 예정 |

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| 백엔드 | Java 25, Spring Boot 4.1, Spring Security (OAuth2 Resource Server, JWT), Spring Data JPA |
| DB | MariaDB 11.8, Flyway |
| API 문서 | springdoc-openapi (Swagger UI) |
| 실행 환경 | Docker Compose |
| 프론트엔드 (예정) | React, TypeScript |

## 개발 환경 실행

필요한 것: JDK 25, Docker (macOS는 OrbStack 권장)

1. DB 실행: `docker compose -f deploy/compose.dev.yaml up -d --wait`
2. 백엔드 실행: `cd backend && ./gradlew bootRun`
3. 개발용 관리자 계정은 서버가 시작할 때 자동으로 만들어진다: `admin` / `admin`

DB를 초기화하려면 `docker compose -f deploy/compose.dev.yaml down -v`로 지운 뒤 다시 띄우고, 백엔드를 재시작한다. 테이블은 백엔드가 시작할 때 Flyway가 만든다.

## API 문서

백엔드 실행 후 http://localhost:8080/swagger-ui.html 에서 모든 API를 보고 바로 호출할 수 있다.

1. `POST /api/auth/login`을 **Try it out**으로 보내 로그인한다.
2. 응답의 `accessToken`을 오른쪽 위 **Authorize**에 넣는다. `Bearer `는 붙이지 않는다.
3. 나머지 API를 **Try it out**으로 호출한다.

## 프로젝트 구조

```
serverdock/
├── backend/    Spring Boot API
├── frontend/   React (예정)
├── deploy/     Docker Compose 설정
└── docs/       문서
```

백엔드는 기능 단위로 패키지를 나눴다.

| 패키지 | 역할 |
| --- | --- |
| `common` | 보안 설정, JWT, 공통 예외 처리 |
| `user` | 계정, 로그인, 관리자 자동 생성 |
| `server` | 서버 설정, 멤버와 역할, 서버별 권한 검사 |
| `invite` | 초대 링크 생성, 폐기, 수락 |
| `docker`, `backup`, `discord` | 다음 단계에서 구현 |
