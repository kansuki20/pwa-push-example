package com.example.push;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final WebPushConfig config;
    private final PushNotificationService pushService;

    public PushController(WebPushConfig config, PushNotificationService pushService) {
        this.config = config;
        this.pushService = pushService;
    }

    /** 1) 프론트가 구독 전에 VAPID 공개키를 받아감 */
    @GetMapping("/public-key")
    public Map<String, String> publicKey() {
        return Map.of("publicKey", config.getPublicKey());
    }

    /** 2) 브라우저가 만든 구독 정보를 저장 */
    @PostMapping("/subscribe")
    public void subscribe(@RequestBody PushSubscriptionDto sub) {
        pushService.subscribe(sub);
    }

    @PostMapping("/unsubscribe")
    public void unsubscribe(@RequestBody Map<String, String> body) {
        pushService.unsubscribe(body.get("endpoint"));
    }

    /**
     * 3) 테스트 발송. 실제로는 "주문 접수" 같은 이벤트 발생 시 서비스 코드에서 호출하면 됨
     * curl -X POST localhost:8080/api/push/send -H 'Content-Type: application/json' \
     *      -d '{"title":"새 주문","body":"주문 #1234가 접수됐어요","url":"/"}'
     */
    @PostMapping("/send")
    public Map<String, Object> send(@RequestBody Map<String, String> body) {
        int ok = pushService.sendToAll(
                body.getOrDefault("title", "테스트 알림"),
                body.getOrDefault("body", "푸시가 잘 오나요?"),
                body.getOrDefault("url", "/"));
        return Map.of("sent", ok, "total", pushService.count());
    }
}
