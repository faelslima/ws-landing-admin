package br.eti.logos.service.pagbank.impl;

import br.eti.logos.entity.landing.Pagamento;
import br.eti.logos.entity.landing.WebhookEvent;
import br.eti.logos.enums.*;
import br.eti.logos.repository.AssinaturaRepository;
import br.eti.logos.repository.PagamentoRepository;
import br.eti.logos.repository.WebhookEventRepository;
import br.eti.logos.service.onboarding.OnboardingService;
import br.eti.logos.service.pagbank.WebhookProcessorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookProcessorServiceImpl implements WebhookProcessorService {

    private final WebhookEventRepository webhookEventRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final OnboardingService onboardingService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${pagbank.token}")
    private String pagbankToken;

    @Value("${rabbitmq.landing.exchange}")
    private String exchange;

    @Value("${rabbitmq.landing.subscription.webhook.routing.key}")
    private String webhookRoutingKey;

    @Override
    @Transactional
    public void processarWebhook(Map<String, String> headers, String payload) {
        var payloadHash = DigestUtils.sha256Hex(payload);

        // Idempotency check via PostgreSQL
        if (webhookEventRepository.existsByPayloadHash(payloadHash)) {
            log.info("Webhook duplicado ignorado: {}", payloadHash.substring(0, 12));
            return;
        }

        // Validate authenticity
        var authenticityToken = headers.get("x-authenticity-token");
        if (authenticityToken != null) {
            var expectedHash = DigestUtils.sha256Hex(pagbankToken + "-" + payload);
            if (!authenticityToken.equalsIgnoreCase(expectedHash)) {
                log.warn("Webhook com assinatura inválida");
                return;
            }
        }

        // Save event record (unique constraint on payloadHash prevents duplicates)
        try {
            var event = WebhookEvent.builder()
                    .payload(payload)
                    .payloadHash(payloadHash)
                    .tipo(detectarTipoEvento(payload))
                    .processado(false)
                    .build();
            webhookEventRepository.save(event);

            // Publish to RabbitMQ for async processing
            rabbitTemplate.convertAndSend(exchange, webhookRoutingKey, payload);
            log.info("Webhook recebido e enfileirado: tipo={}", event.getTipo());
        } catch (Exception e) {
            // Constraint violation = duplicate (race condition)
            log.info("Webhook duplicado ignorado (race condition): {}", payloadHash.substring(0, 12));
        }
    }

    @Override
    @Transactional
    public void processarEventoAsync(String payload) {
        try {
            var json = objectMapper.readTree(payload);
            var tipo = detectarTipoEvento(payload);

            switch (tipo) {
                case SUBSCRIPTION_ACTIVATED -> processarSubscriptionActivated(json);
                case SUBSCRIPTION_CANCELED -> processarSubscriptionCanceled(json);
                case SUBSCRIPTION_SUSPENDED -> processarSubscriptionSuspended(json);
                case INVOICE_PAID -> processarInvoicePaid(json);
                case INVOICE_OVERDUE -> processarInvoiceOverdue(json);
                case INVOICE_REFUNDED -> processarInvoiceRefunded(json);
                default -> log.info("Evento não tratado: {}", tipo);
            }

            // Mark as processed
            var payloadHash = DigestUtils.sha256Hex(payload);
            webhookEventRepository.findByPayloadHash(payloadHash).ifPresent(event -> {
                event.setProcessado(true);
                event.setProcessadoEm(OffsetDateTime.now());
                webhookEventRepository.save(event);
            });

        } catch (Exception e) {
            log.error("Erro ao processar webhook: {}", e.getMessage(), e);
            var payloadHash = DigestUtils.sha256Hex(payload);
            webhookEventRepository.findByPayloadHash(payloadHash).ifPresent(event -> {
                event.setErroProcessamento(e.getMessage());
                webhookEventRepository.save(event);
            });
            throw new RuntimeException("Falha no processamento do webhook", e);
        }
    }

    private void processarSubscriptionActivated(JsonNode json) {
        var subscriptionId = json.path("id").asText();
        log.info("Subscription ativada: {}", subscriptionId);
        onboardingService.processarPagamentoConfirmado(subscriptionId);
    }

    private void processarSubscriptionCanceled(JsonNode json) {
        var subscriptionId = json.path("id").asText();
        assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).ifPresent(assinatura -> {
            assinatura.setStatus(AssinaturaStatusEnum.CANCELED);
            assinatura.setDataCancelamento(OffsetDateTime.now());
            assinaturaRepository.save(assinatura);

            var licenca = assinatura.getLicenca();
            licenca.setStatus(LicencaStatusEnum.CANCELADA);
            licenca.setDataCancelamento(OffsetDateTime.now());
        });
        log.info("Subscription cancelada via webhook: {}", subscriptionId);
    }

    private void processarSubscriptionSuspended(JsonNode json) {
        var subscriptionId = json.path("id").asText();
        assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).ifPresent(assinatura -> {
            assinatura.setStatus(AssinaturaStatusEnum.SUSPENDED);
            assinaturaRepository.save(assinatura);
        });
        log.info("Subscription suspensa via webhook: {}", subscriptionId);
    }

    private void processarInvoicePaid(JsonNode json) {
        var invoiceId = json.path("id").asText();
        var subscriptionId = json.path("subscription_id").asText();
        var valor = json.path("amount").path("value").asInt();

        var assinatura = assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).orElse(null);
        if (assinatura == null) {
            log.warn("Assinatura não encontrada para invoice: {}", invoiceId);
            return;
        }

        // Avoid duplicate
        if (pagamentoRepository.findByPagbankInvoiceId(invoiceId).isPresent()) {
            return;
        }

        var pagamento = Pagamento.builder()
                .assinatura(assinatura)
                .pagbankInvoiceId(invoiceId)
                .status(PagamentoStatusEnum.PAID)
                .valor(BigDecimal.valueOf(valor))
                .formaPagamento(FormaPagamentoEnum.CREDIT_CARD)
                .dataPagamento(OffsetDateTime.now())
                .build();
        pagamentoRepository.save(pagamento);

        // Update next invoice date
        assinatura.setDataProximaFatura(OffsetDateTime.now().plusYears(1));
        assinaturaRepository.save(assinatura);

        log.info("Invoice paga registrada: {} - Valor: {}", invoiceId, valor);
    }

    private void processarInvoiceOverdue(JsonNode json) {
        var invoiceId = json.path("id").asText();
        var subscriptionId = json.path("subscription_id").asText();

        assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).ifPresent(assinatura -> {
            assinatura.setStatus(AssinaturaStatusEnum.OVERDUE);
            assinaturaRepository.save(assinatura);
        });

        pagamentoRepository.findByPagbankInvoiceId(invoiceId).ifPresent(pagamento -> {
            pagamento.setStatus(PagamentoStatusEnum.DECLINED);
            pagamentoRepository.save(pagamento);
        });

        log.warn("Invoice vencida: {} - subscription: {}", invoiceId, subscriptionId);
    }

    private void processarInvoiceRefunded(JsonNode json) {
        var invoiceId = json.path("id").asText();
        pagamentoRepository.findByPagbankInvoiceId(invoiceId).ifPresent(pagamento -> {
            pagamento.setStatus(PagamentoStatusEnum.REFUNDED);
            pagamento.setDataEstorno(OffsetDateTime.now());
            pagamento.setValorEstornado(pagamento.getValor());
            pagamentoRepository.save(pagamento);
        });
        log.info("Invoice estornada via webhook: {}", invoiceId);
    }

    private WebhookEventTypeEnum detectarTipoEvento(String payload) {
        try {
            var json = objectMapper.readTree(payload);
            var status = json.path("status").asText().toUpperCase();
            var hasSubscriptionId = json.has("subscription_id") || json.path("id").asText().startsWith("SUB");

            if (hasSubscriptionId || json.has("plan")) {
                return switch (status) {
                    case "ACTIVE" -> WebhookEventTypeEnum.SUBSCRIPTION_ACTIVATED;
                    case "CANCELED" -> WebhookEventTypeEnum.SUBSCRIPTION_CANCELED;
                    case "SUSPENDED" -> WebhookEventTypeEnum.SUBSCRIPTION_SUSPENDED;
                    default -> WebhookEventTypeEnum.SUBSCRIPTION_CREATED;
                };
            } else {
                return switch (status) {
                    case "PAID" -> WebhookEventTypeEnum.INVOICE_PAID;
                    case "OVERDUE" -> WebhookEventTypeEnum.INVOICE_OVERDUE;
                    case "REFUNDED" -> WebhookEventTypeEnum.INVOICE_REFUNDED;
                    case "CANCELED" -> WebhookEventTypeEnum.INVOICE_CANCELED;
                    default -> WebhookEventTypeEnum.INVOICE_CREATED;
                };
            }
        } catch (Exception e) {
            return WebhookEventTypeEnum.SUBSCRIPTION_CREATED;
        }
    }
}
