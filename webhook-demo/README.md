# Webhook Demo (Spring Boot)

A minimal Spring Boot project to learn webhook communication with HMAC signing.

## Run

```bash
cd webhook-demo
mvn spring-boot:run
```

## Endpoints

- POST `/webhook` — receiver verifying `X-Signature` (HMAC-SHA256 of body with shared secret)
- POST `/send-test` — sender that signs and delivers the payload to `url` query param or the configured default
- POST `/echo` — local echo helper

## Configuration

Configured in `src/main/resources/application.yml`:

- `webhook.secret`: shared secret for signing/verification
- `webhook.defaultTargetUrl`: default target for `/send-test`

## Quick demo (loopback)

In a separate terminal after the app is running:

```bash
curl -X POST "http://localhost:8080/send-test" \
  -H 'Content-Type: application/json' \
  -d '{"hello":"world"}'
```

This posts to the local `/webhook` with a valid signature. You should see "Webhook received".

## Send to an external URL

```bash
curl -X POST "http://localhost:8080/send-test?url=https://httpbin.org/post" \
  -H 'Content-Type: application/json' \
  -d '{"event":"user.created","id":123}'
```

## Receiving from external services

Expose your local app using a tunnel (e.g., `ngrok`):

```bash
ngrok http 8080
```

Configure the external service to call `https://<your-domain>/webhook` and share the secret you set.