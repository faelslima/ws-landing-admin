package br.eti.logos.controller.webhook;

import br.eti.logos.service.pagbank.WebhookProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestBody String payload) {
        try {
            webhookProcessorService.processarWebhook(headers, payload);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            log.warn("Webhook rejeitado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
