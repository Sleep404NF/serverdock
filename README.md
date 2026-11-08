# ServerDock

친구끼리 쓰는 마인크래프트 서버 관리 도구 (셀프호스팅)

## 개발 환경 실행

1. DB 실행: `docker compose -f deploy/compose.dev.yaml up -d --wait`
2. 백엔드 실행: `cd backend && ./gradlew bootRun`
3. 개발용 관리자 계정은 서버가 시작할 때 자동으로 만들어집니다: `admin` / `admin`

DB를 초기화하려면 `docker compose -f deploy/compose.dev.yaml down -v`로 지운 뒤 다시 띄우고, 백엔드를 재시작하세요. 테이블은 백엔드가 시작할 때 Flyway가 만듭니다.

## API 문서

백엔드 실행 후 http://localhost:8080/swagger-ui.html 에서 모든 API를 보고 바로 호출할 수 있습니다.

1. `POST /api/auth/login`을 **Try it out**으로 보내 로그인합니다.
2. 응답의 `accessToken`을 오른쪽 위 **Authorize**에 넣습니다. `Bearer `는 붙이지 않습니다.
3. 나머지 API를 **Try it out**으로 호출합니다.
