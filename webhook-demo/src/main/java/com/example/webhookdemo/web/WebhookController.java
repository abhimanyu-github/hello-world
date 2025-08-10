package com.example.webhookdemo.web;

import com.example.webhookdemo.service.WebhookSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookSenderService webhookSenderService;

    @Value("${webhook.secret}")
    private String webhookSecret;

    @Value("${webhook.defaultTargetUrl}")
    private String defaultTargetUrl;

    public WebhookController(WebhookSenderService webhookSenderService) {
        this.webhookSenderService = webhookSenderService;
    }

    @PostMapping(path = "/webhook", consumes = {"application/json", "*/*"})
    public ResponseEntity<?> receiveWebhook(@RequestHeader(value = "X-Signature", required = false) String signature,
                                            @RequestBody(required = false) String body,
                                            @RequestHeader Map<String, String> headers) {
        if (!StringUtils.hasText(body)) {
            body = "";
        }
        if (!StringUtils.hasText(signature)) {
            log.warn("Missing X-Signature header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing signature");
        }
        String expected = computeSignature(body, webhookSecret);
        if (!expected.equals(signature)) {
            log.warn("Invalid signature. expected={}, actual={}", expected, signature);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }
        log.info("Received webhook with headers={}, body={}", headers, body);
        return ResponseEntity.ok("Webhook received");
    }

    @PostMapping(path = "/echo", consumes = {"application/json", "*/*"})
    public ResponseEntity<?> echo(@RequestBody(required = false) String body,
                                  @RequestHeader Map<String, String> headers) {
        if (!StringUtils.hasText(body)) {
            body = "";
        }
        log.info("Echo endpoint: headers={}, body={}", headers, body);
        return ResponseEntity.ok(Map.of("echo", body));
    }

    @PostMapping(path = "/send-test", consumes = {"application/json", "*/*"})
    public ResponseEntity<?> sendTest(@RequestParam(value = "url", required = false) String url,
                                      @RequestBody(required = false) String body) {
        String targetUrl = StringUtils.hasText(url) ? url : defaultTargetUrl;
        String payload = StringUtils.hasText(body) ? body : "{\"message\":\"Hello from webhook demo\"}";
        String response = webhookSenderService.sendWebhook(targetUrl, payload);
        return ResponseEntity.ok(Map.of("targetUrl", targetUrl, "payload", payload, "response", response));
    }

    private String computeSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute HMAC signature", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}