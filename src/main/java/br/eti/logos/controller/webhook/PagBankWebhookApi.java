package br.eti.logos.controller.webhook;

import br.eti.logos.service.pagbank.WebhookProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhooks/pagbank")
@RequiredArgsConstructor
public class PagBankWebhookApi {

    private final WebhookProcessorService webhookProcessorService;

    @PostMapping("/subscriptions")
    public ResponseEntity<Void> receberNotificacaoSubscription(
            @RequestHeader Map<String, String> headers,
            @RequestBody String payload) {
        webhookProcessorService.processarWebhook(headers, payload);
        return ResponseEntity.ok().build();
    }
}
