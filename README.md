# PWA 설치 + 웹 푸시 최소 예제 (Spring Boot)

## 해야 하는 것 (전체 그림)

| # | 할 일 | 파일 |
|---|------|------|
| 1 | **manifest** — 앱 이름/아이콘/standalone 모드 → "설치" 가능해짐 | `static/manifest.json`, `static/icons/` |
| 2 | **Service Worker** — 백그라운드에서 푸시 받아 알림 띄움 | `static/sw.js` (반드시 루트 경로) |
| 3 | **VAPID 키쌍** — 서버 신원 증명용. 공개키는 프론트, 개인키는 서버 | `WebPushConfig.java`, `application.yml` |
| 4 | **구독** — 권한 요청 → `pushManager.subscribe()` → endpoint/키를 서버에 저장 | `index.html`, `PushController` |
| 5 | **발송** — 저장된 구독으로 VAPID 서명해서 보냄. 404/410 오면 구독 삭제 | `PushNotificationService.java` |
| 6 | **HTTPS** — 운영 필수. `localhost`는 예외로 http 허용 | — |

## 실행

```bash
./gradlew bootRun     # 또는 IDE에서 PushApplication 실행
```

1. 첫 실행 로그에 **VAPID 키**가 찍힘 → `application.yml`에 붙여넣고 재시작
   (안 하면 재시작할 때마다 키가 바뀌어서 기존 구독이 다 무효됨)
2. Chrome에서 `http://localhost:8080` 열기
3. **알림 받기** → 권한 허용
4. **테스트 푸시 보내기** (또는 아래 curl) → 알림 뜨면 성공

```bash
curl -X POST localhost:8080/api/push/send \
  -H 'Content-Type: application/json' \
  -d '{"title":"새 주문","body":"주문 #1234가 접수됐어요","url":"/"}'
```

탭을 닫아도 브라우저만 켜져 있으면 알림이 옴 (Service Worker가 깨어남).

## 폰에서 테스트하려면

localhost가 아니면 HTTPS가 필요함. 제일 간단한 건 터널:

```bash
ngrok http 8080        # 또는 cloudflared tunnel --url http://localhost:8080
```

- **안드로이드 Chrome**: 링크 열고 → "앱 설치" → 알림 받기
- **iPhone (iOS 16.4+)**: Safari로 열고 → 공유 → **홈 화면에 추가** → 홈 화면 아이콘으로 실행한 뒤 알림 받기
  (Safari 탭 상태에선 `PushManager` 자체가 없음)

## 실제 서비스에 붙일 때

- 구독은 메모리 Map → **DB 테이블**로:
  `push_subscription(id, user_id, endpoint UNIQUE, p256dh, auth, created_at)`
- 로그인 사용자와 연결해서 저장 → "특정 사용자에게 보내기" 가능
- 발송은 주문 접수 같은 이벤트에서 호출. 대상 많으면 `@Async`나 큐로 분리
- VAPID 개인키는 환경변수/시크릿으로 (`VAPID_PRIVATE_KEY`)

## 컴파일 에러 나면

`org.bouncycastle...` 이나 `org.apache.http...` 를 못 찾는다고 하면,
web-push의 전이 의존성이 컴파일 클래스패스에 안 올라온 것. `build.gradle`에 추가:

```groovy
implementation 'org.bouncycastle:bcprov-jdk18on:1.78.1'
implementation 'org.apache.httpcomponents:httpasyncclient:4.1.5'
```
