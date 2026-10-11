package com.example.push;

/**
 * 브라우저의 PushSubscription.toJSON() 결과 그대로 받는 DTO.
 * {
 *   "endpoint": "https://fcm.googleapis.com/fcm/send/...",
 *   "keys": { "p256dh": "...", "auth": "..." }
 * }
 */
public record PushSubscriptionDto(String endpoint, Keys keys) {
    public record Keys(String p256dh, String auth) {}
}
