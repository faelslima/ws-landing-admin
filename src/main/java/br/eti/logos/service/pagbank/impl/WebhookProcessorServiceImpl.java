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
    private final br.eti.logos.service.pagbank.PagBankService pagBankService;

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

        // Validate authenticity — rejeita se header ausente ou hash divergente
        var authenticityToken = headers.get("x-authenticity-token");
        if (authenticityToken == null || authenticityToken.isBlank()) {
            log.warn("Webhook rejeitado: header x-authenticity-token ausente");
            throw new SecurityException("Webhook sem assinatura");
        }
        var expectedHash = DigestUtils.sha256Hex(pagbankToken + "-" + payload);
        if (!org.apache.commons.lang3.StringUtils.equalsIgnoreCase(authenticityToken, expectedHash)) {
            log.warn("Webhook rejeitado: assinatura inválida");
            throw new SecurityException("Assinatura de webhook inválida");
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
                case SUBSCRIPTION_INITIAL, SUBSCRIPTION_ACTIVATED -> processarSubscriptionActivated(json);
                case SUBSCRIPTION_CANCELED, SUBSCRIPTION_EXPIRED -> processarSubscriptionCanceled(json);
                case SUBSCRIPTION_SUSPENDED -> processarSubscriptionSuspended(json);
                case SUBSCRIPTION_RECURRENCE -> processarSubscriptionRecurrence(json);
                case INVOICE_CREATED -> processarInvoiceCreated(json);
                case INVOICE_PAID -> processarInvoicePaid(json);
                case INVOICE_OVERDUE -> processarInvoiceOverdue(json);
                case INVOICE_REFUNDED, REFUND_CREATED -> processarInvoiceRefunded(json);
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
        var resource = json.has("resource") ? json.path("resource") : json;
        var subscriptionId = resource.path("id").asText();
        log.info("Subscription ativada: {}", subscriptionId);
        onboardingService.processarPagamentoConfirmado(subscriptionId);
    }

    private void processarSubscriptionCanceled(JsonNode json) {
        var resource = json.has("resource") ? json.path("resource") : json;
        var subscriptionId = resource.path("id").asText();
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
        var resource = json.has("resource") ? json.path("resource") : json;
        var subscriptionId = resource.path("id").asText();
        assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).ifPresent(assinatura -> {
            assinatura.setStatus(AssinaturaStatusEnum.SUSPENDED);
            assinaturaRepository.save(assinatura);
        });
        log.info("Subscription suspensa via webhook: {}", subscriptionId);
    }

    private void processarSubscriptionRecurrence(JsonNode json) {
        // Evento de cobrança recorrente bem-sucedida
        var resource = json.has("resource") ? json.path("resource") : json;
        var subscriptionId = resource.path("id").asText();

        assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).ifPresent(assinatura -> {
            // Atualizar status se estava OVERDUE
            if (assinatura.getStatus() == AssinaturaStatusEnum.OVERDUE) {
                assinatura.setStatus(AssinaturaStatusEnum.ACTIVE);
            }

            // Atualizar data da próxima fatura
            var nextInvoiceAt = resource.path("next_invoice_at").asText();
            if (!nextInvoiceAt.isEmpty()) {
                try {
                    assinatura.setDataProximaFatura(OffsetDateTime.parse(nextInvoiceAt));
                } catch (Exception e) {
                    log.warn("Erro ao parsear next_invoice_at: {}", nextInvoiceAt);
                }
            }
            assinaturaRepository.save(assinatura);

            // Buscar invoices para registrar pagamento
            try {
                var invoices = pagBankService.listarFaturas(subscriptionId);
                if (invoices != null && !invoices.isEmpty()) {
                    var ultimaInvoice = invoices.stream()
                        .filter(inv -> "PAID".equals(inv.getStatus()))
                        .findFirst();

                    if (ultimaInvoice.isPresent() && pagamentoRepository.findByPagbankInvoiceId(ultimaInvoice.get().getId()).isEmpty()) {
                        var invoice = ultimaInvoice.get();
                        var valorCentavos = invoice.getAmount() != null && invoice.getAmount().getValue() != null
                            ? invoice.getAmount().getValue()
                            : 0;

                        var pagamento = Pagamento.builder()
                            .assinatura(assinatura)
                            .pagbankInvoiceId(invoice.getId())
                            .status(PagamentoStatusEnum.PAID)
                            .valor(BigDecimal.valueOf(valorCentavos).divide(BigDecimal.valueOf(100)))
                            .formaPagamento(FormaPagamentoEnum.CREDIT_CARD)
                            .dataPagamento(OffsetDateTime.now())
                            .build();
                        pagamentoRepository.save(pagamento);

                        log.info("Cobrança recorrente registrada: {} - subscription: {}", invoice.getId(), subscriptionId);
                    }
                }
            } catch (Exception e) {
                log.error("Erro ao buscar invoices da recorrência {}: {}", subscriptionId, e.getMessage());
            }
        });
    }

    private void processarInvoiceCreated(JsonNode json) {
        // Evento disparado quando uma nova fatura é criada (inicial ou recorrente)
        var resource = json.has("resource") ? json.path("resource") : json;
        var invoiceId = resource.path("id").asText();
        var subscriptionId = resource.path("subscription_id").asText();

        assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).ifPresent(assinatura -> {
            // Verificar se já existe pagamento para esta invoice
            if (pagamentoRepository.findByPagbankInvoiceId(invoiceId).isEmpty()) {
                var valorCentavos = resource.path("amount").path("value").asInt(0);

                var pagamento = Pagamento.builder()
                    .assinatura(assinatura)
                    .pagbankInvoiceId(invoiceId)
                    .status(PagamentoStatusEnum.PENDING)
                    .valor(BigDecimal.valueOf(valorCentavos).divide(BigDecimal.valueOf(100)))
                    .formaPagamento(FormaPagamentoEnum.CREDIT_CARD)
                    .build();
                pagamentoRepository.save(pagamento);

                log.info("Invoice criada e registrada: {} - subscription: {} - valor: R$ {}",
                    invoiceId, subscriptionId, pagamento.getValor());
            }
        });
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
        var resource = json.has("resource") ? json.path("resource") : json;
        var subscriptionId = resource.path("id").asText();

        // Status OVERDUE no webhook de subscription indica problema de pagamento
        assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).ifPresent(assinatura -> {
            assinatura.setStatus(AssinaturaStatusEnum.OVERDUE);
            assinaturaRepository.save(assinatura);

            // Buscar invoices para obter detalhes do erro
            try {
                var invoices = pagBankService.listarFaturas(subscriptionId);
                if (invoices != null && !invoices.isEmpty()) {
                    // Pegar a invoice mais recente com problema
                    var invoiceComProblema = invoices.stream()
                        .filter(inv -> "OVERDUE".equals(inv.getStatus()) || "UNPAID".equals(inv.getStatus()))
                        .findFirst();

                    if (invoiceComProblema.isPresent()) {
                        var invoice = invoiceComProblema.get();
                        var motivoRecusa = extrairMotivoRecusaDeInvoice(invoice);

                        // Criar ou atualizar pagamento com status DECLINED
                        var valorCentavos = invoice.getAmount() != null && invoice.getAmount().getValue() != null
                            ? invoice.getAmount().getValue()
                            : 0;
                        var pagamento = pagamentoRepository.findByPagbankInvoiceId(invoice.getId())
                            .orElseGet(() -> Pagamento.builder()
                                .assinatura(assinatura)
                                .pagbankInvoiceId(invoice.getId())
                                .valor(BigDecimal.valueOf(valorCentavos).divide(BigDecimal.valueOf(100)))
                                .formaPagamento(FormaPagamentoEnum.CREDIT_CARD)
                                .build());

                        pagamento.setStatus(PagamentoStatusEnum.DECLINED);
                        pagamento.setMotivoRecusa(motivoRecusa);
                        pagamentoRepository.save(pagamento);

                        log.warn("Invoice vencida: {} - subscription: {} - motivo: {}", invoice.getId(), subscriptionId, motivoRecusa);
                    }
                }
            } catch (Exception e) {
                log.error("Erro ao buscar invoices da subscription {}: {}", subscriptionId, e.getMessage());
            }
        });
    }

    private String extrairMotivoRecusaDeInvoice(br.eti.logos.dto.pagbank.PagBankInvoiceDto invoice) {
        // Tentar extrair código e mensagem de erro da invoice
        if (invoice.getDeclineReason() != null && !invoice.getDeclineReason().isEmpty()) {
            return invoice.getDeclineReason();
        }

        if (invoice.getPaymentResponse() != null) {
            var code = invoice.getPaymentResponse().getCode();
            var message = invoice.getPaymentResponse().getMessage();
            if (code != null || message != null) {
                return String.format("Código: %s - %s",
                    code != null ? code : "N/A",
                    message != null ? message : "Recusado pela operadora");
            }
        }

        // Fallback genérico
        return "Pagamento recusado - verifique dados do cartão";
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

            // Formato oficial: campo "event" com valores como "subscription.activated"
            if (json.has("event")) {
                var event = json.path("event").asText().toUpperCase().replace(".", "_");
                try {
                    return WebhookEventTypeEnum.valueOf(event);
                } catch (IllegalArgumentException e) {
                    log.warn("Evento desconhecido: {}", event);
                    return WebhookEventTypeEnum.SUBSCRIPTION_UPDATED;
                }
            }

            // Fallback para formato antigo (compatibilidade)
            var status = json.path("resource").path("status").asText().toUpperCase();
            if (status.isEmpty()) {
                status = json.path("status").asText().toUpperCase();
            }

            var hasSubscriptionId = json.has("subscription_id") || json.path("resource").path("id").asText().startsWith("SUBS");

            if (hasSubscriptionId || json.path("resource").has("plan")) {
                return switch (status) {
                    case "ACTIVE" -> WebhookEventTypeEnum.SUBSCRIPTION_ACTIVATED;
                    case "CANCELED" -> WebhookEventTypeEnum.SUBSCRIPTION_CANCELED;
                    case "SUSPENDED" -> WebhookEventTypeEnum.SUBSCRIPTION_SUSPENDED;
                    case "OVERDUE" -> WebhookEventTypeEnum.SUBSCRIPTION_RECURRENCE;
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
            log.error("Erro ao detectar tipo de evento: {}", e.getMessage());
            return WebhookEventTypeEnum.SUBSCRIPTION_UPDATED;
        }
    }
}
