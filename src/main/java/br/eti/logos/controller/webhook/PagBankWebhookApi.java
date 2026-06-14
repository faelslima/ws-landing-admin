package br.eti.logos.controller.webhook;

import br.eti.logos.service.pagbank.WebhookProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/webhooks/pagbank")
@RequiredArgsConstructor
public class PagBankWebhookApi {

    private final WebhookProcessorService webhookProcessorService;

    @PostMapping("/subscriptions")
    public ResponseEntity<Void> receberNotificacaoSubscription(
            @RequestHeader Map<String, String> headers,
            @RequestHeader(value = "x-payload-signature", required = false) String payloadSignature,
            @RequestBody String payload) {

//        salvarDebugWebhook(headers, payload);

        try {
            webhookProcessorService.processarWebhook(payloadSignature, payload);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            log.warn("Webhook rejeitado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private void salvarDebugWebhook(Map<String, String> headers, String payload) {
        try {
            Path path = Paths.get("webhook-debug.md");
            StringBuilder sb = new StringBuilder();
            sb.append("## Webhook recebido em ").append(OffsetDateTime.now()).append("\n\n");
            sb.append("### Headers\n\n");
            headers.forEach((k, v) -> sb.append("- **").append(k).append(":** ").append(v).append("\n"));
            sb.append("\n### Payload\n\n```json\n").append(payload).append("\n```\n\n---\n\n");
            Files.writeString(path, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("Falha ao salvar debug do webhook: {}", e.getMessage());
        }
    }
}
