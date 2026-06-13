package br.eti.logos.service.pagbank;

public interface WebhookProcessorService {

    void processarWebhook(String payloadSignature, String payload);

    void processarEventoAsync(String payload);
}
