package com.example.push;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.util.Base64;

import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.BigIntegers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * VAPID 키 + PushService 설정.
 *
 * VAPID 키 = "이 푸시는 우리 서버가 보낸 것"을 증명하는 서버 전용 키쌍.
 *  - 공개키: 브라우저가 구독할 때 사용 (프론트에 내려줌)
 *  - 개인키: 서버가 푸시 보낼 때 서명용 (절대 노출 X)
 *
 * application.yml 에 키가 없으면 실행 시 새로 만들어서 로그에 찍어줌.
 * → 그 값을 yml 에 붙여넣어 고정시켜야 함. (키가 바뀌면 기존 구독이 전부 무효가 됨)
 */
@Configuration
public class WebPushConfig {

    private static final Logger log = LoggerFactory.getLogger(WebPushConfig.class);

    @Value("${webpush.vapid.public-key:}")
    private String publicKey;

    @Value("${webpush.vapid.private-key:}")
    private String privateKey;

    @Value("${webpush.vapid.subject}")
    private String subject;

    @Bean
    public PushService pushService() throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());

        if (publicKey.isBlank() || privateKey.isBlank()) {
            KeyPair kp = generateVapidKeyPair();
            publicKey = b64(((ECPublicKey) kp.getPublic()).getQ().getEncoded(false));
            privateKey = b64(BigIntegers.asUnsignedByteArray(32, ((ECPrivateKey) kp.getPrivate()).getS()));
            log.warn("""

                    ===== VAPID 키가 설정되지 않아 새로 생성했습니다. application.yml 에 붙여넣으세요 =====
                    webpush:
                      vapid:
                        public-key: {}
                        private-key: {}
                    =====================================================================================
                    """, publicKey, privateKey);
        }

        PushService service = new PushService(publicKey, privateKey);
        service.setSubject(subject);
        return service;
    }

    /** 프론트에 내려줄 공개키 */
    public String getPublicKey() {
        return publicKey;
    }

    private static KeyPair generateVapidKeyPair() throws GeneralSecurityException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        gen.initialize(ECNamedCurveTable.getParameterSpec("prime256v1"));
        return gen.generateKeyPair();
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
