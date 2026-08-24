# EARTHY Shop

IntelliJ에서 열 수 있는 Spring Boot 프로젝트입니다.

## 실행 방법

1. IntelliJ IDEA에서 `outputs/earthy-intellij` 폴더를 엽니다.
2. Gradle import가 끝날 때까지 기다립니다.
3. `src/main/java/com/earthy/shop/EarthyShopApplication.java`를 실행합니다.
4. 백엔드는 `http://localhost:8080`에서 실행됩니다.
5. 프론트엔드는 `frontend/`의 React/Vite 앱을 실행해 접속합니다.

현재 운영 프론트엔드는 `frontend/` 디렉터리의 React/Vite 앱 기준입니다.
Spring Boot의 `src/main/resources/static`은 상품 이미지 등 정적 asset 보관 용도로만 사용합니다.

## 다음 단계

- S3 상품 이미지 업로드 운영 연결
- EC2 백엔드 배포
- Vercel 프론트 배포
- 운영 도메인/HTTPS 연결
- Toss Payments 운영키 전환
