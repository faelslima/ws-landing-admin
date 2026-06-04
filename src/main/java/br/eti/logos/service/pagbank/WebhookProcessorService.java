package br.eti.logos.service.pagbank;

import java.util.Map;

public interface WebhookProcessorService {

    void processarWebhook(Map<String, String> headers, String payload);

    void processarEventoAsync(String payload);
}
