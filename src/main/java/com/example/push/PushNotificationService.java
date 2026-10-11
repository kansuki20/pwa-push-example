package com.example.push;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 구독 저장 + 발송.
 *
 * 예제라서 구독을 메모리(Map)에 저장함. 실제로는 DB 테이블로:
 *   push_subscription(id, user_id, endpoint UNIQUE, p256dh, auth, created_at)
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    /** key = endpoint (브라우저/기기마다 고유) */
    private final Map<String, PushSubscriptionDto> subscriptions = new ConcurrentHashMap<>();

    private final nl.martijndwars.webpush.PushService webPush;
    private final ObjectMapper objectMapper;

    public PushNotificationService(nl.martijndwars.webpush.PushService webPush, ObjectMapper objectMapper) {
        this.webPush = webPush;
        this.objectMapper = objectMapper;
    }

    public void subscribe(PushSubscriptionDto sub) {
        subscriptions.put(sub.endpoint(), sub);
        log.info("구독 저장. 현재 {}건", subscriptions.size());
    }

    public void unsubscribe(String endpoint) {
        subscriptions.remove(endpoint);
    }

    public int count() {
        return subscriptions.size();
    }

    /** 저장된 모든 구독자에게 발송. 성공 건수 반환 */
    public int sendToAll(String title, String body, String url) {
        String payload;
        try {
            // sw.js 의 push 이벤트에서 이 JSON 을 꺼내 씀
            payload = objectMapper.writeValueAsString(Map.of("title", title, "body", body, "url", url));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        int success = 0;
        for (PushSubscriptionDto sub : subscriptions.values()) {
            try {
                Subscription target = new Subscription(
                        sub.endpoint(),
                        new Subscription.Keys(sub.keys().p256dh(), sub.keys().auth()));

                HttpResponse res = webPush.send(new Notification(target, payload));
                int status = res.getStatusLine().getStatusCode();

                if (status == 201 || status == 200) {
                    success++;
                } else if (status == 404 || status == 410) {
                    // 사용자가 권한을 끄거나 앱 삭제 → 만료된 구독. 지워줘야 함
                    subscriptions.remove(sub.endpoint());
                    log.info("만료된 구독 삭제 ({})", status);
                } else {
                    log.warn("발송 실패 status={}", status);
                }
            } catch (Exception e) {
                log.error("발송 중 오류", e);
            }
        }
        return success;
    }
}
