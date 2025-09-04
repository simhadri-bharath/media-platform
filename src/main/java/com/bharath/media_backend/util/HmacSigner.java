package com.bharath.media_backend.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class HmacSigner {

    @Value("${app.hmac.secret}")
    private String secret;

    private Mac mac;

    @PostConstruct
    public void init() throws Exception {
        mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
    }

    public String sign(String data) {
        byte[] rawHmac = mac.doFinal(data.getBytes());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
    }
}
