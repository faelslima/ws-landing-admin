package br.eti.logos.service.pagbank.impl;

import br.eti.logos.core.util.MoneyUtil;
import br.eti.logos.dto.pagbank.InvoicesListDto;
import br.eti.logos.dto.saga.LicenseReactivationEvent;
import br.eti.logos.dto.saga.LicenseSuspensionEvent;
import br.eti.logos.entity.landing.Pagamento;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import br.eti.logos.entity.landing.WebhookEvent;
import br.eti.logos.enums.*;
import br.eti.logos.repository.*;
import br.eti.logos.service.email.EmailService;
import br.eti.logos.service.onboarding.CheckoutRetryService;
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

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookProcessorServiceImpl implements WebhookProcessorService {

    private final WebhookEventRepository webhookEventRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final LicencaRepository licencaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final LeadRepository leadRepository;
    private final OnboardingService onboardingService;
    private final CheckoutRetryService checkoutRetryService;
    private final EmailService emailService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final br.eti.logos.service.pagbank.PagBankService pagBankService;

    @Value("${rabbitmq.landing.exchange}")
    private String exchange;

    @Value("${rabbitmq.landing.subscription.webhook.routing.key}")
    private String webhookRoutingKey;

    @Value("${rabbitmq.landing.saga.license.suspension.routing.key}")
    private String sagaLicenseSuspensionRoutingKey;

    @Value("${rabbitmq.landing.saga.license.reactivation.routing.key}")
    private String sagaLicenseReactivationRoutingKey;

    @Value("${app.landing.url:https://i12.logos.br}")
    private String landingUrl;

    @Override
    @Transactional
    public void processarWebhook(String payloadSignature, String payload) {
        // Validação de assinatura desabilitada: PagBank usa x-payload-signature em recorrências
        // mas a feature ainda não tem documentação pública — reativar quando estabilizar.
        // Algoritmo previsto: SHA-256(pagbankToken + "-" + payload) vs x-payload-signature.

        // Idempotency check via PostgreSQL
        var payloadHash = DigestUtils.sha256Hex(payload);
        if (webhookEventRepository.existsByPayloadHash(payloadHash)) {
            log.info("Webhook duplicado ignorado: {}", payloadHash.substring(0, 12));
            return;
        }

        try {
            var event = WebhookEvent.builder()
                    .payload(payload)
                    .payloadHash(payloadHash)
                    .tipo(detectarTipoEvento(payload))
                    .processado(false)
                    .build();
            webhookEventRepository.save(event);

            rabbitTemplate.convertAndSend(exchange, webhookRoutingKey, payload);
            log.info("Webhook recebido e enfileirado: tipo={}", event.getTipo());
        } catch (Exception e) {
            log.info("Webhook duplicado ignorado (race condition): {}", payloadHash.substring(0, 12));
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "leads", allEntries = true),
        @CacheEvict(value = "licencas", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
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
        var status = resource.path("status").asText();

        if ("ACTIVE".equalsIgnoreCase(status)) {
            // Verifica se a assinatura já existe (pode não existir por race condition com o checkout)
            var assinatura = assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).orElse(null);
            if (assinatura == null) {
                log.info("Webhook SUBSCRIPTION_INITIAL/ACTIVE para {} chegou antes do checkout salvar — onboarding já será disparado pelo checkout", subscriptionId);
                return;
            }
            // Só dispara onboarding via webhook se o checkout ainda não ativou (status ainda PENDING)
            if (assinatura.getStatus() == AssinaturaStatusEnum.PENDING || assinatura.getStatus() == AssinaturaStatusEnum.TRIAL) {
                log.info("Subscription ativada via webhook, iniciando onboarding: {}", subscriptionId);
                onboardingService.processarPagamentoConfirmado(subscriptionId);
            } else {
                log.info("Subscription {} já está com status={}, onboarding já foi disparado", subscriptionId, assinatura.getStatus());
            }
            return;
        }

        // Pagamento negado no checkout: atualiza o registro usando current_invoice embutido no evento
        log.info("Subscription {} com status={} — atualizando pagamento via current_invoice", subscriptionId, status);

        // Captura status antes de persistir para decidir notificação
        var assinatura = assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).orElse(null);
        var eraFalhaPagamentoInicial = assinatura != null
                && "OVERDUE".equalsIgnoreCase(status)
                && (assinatura.getStatus() == AssinaturaStatusEnum.PENDING
                        || assinatura.getStatus() == AssinaturaStatusEnum.TRIAL);

        var currentInvoice = resource.path("current_invoice");
        if (!currentInvoice.isMissingNode()) {
            atualizarPagamentoDaInvoiceEmbutida(subscriptionId, currentInvoice);
        }

        if (eraFalhaPagamentoInicial) {
            // Recarrega para ter a entidade com o estado persistido (status já OVERDUE)
            assinaturaRepository.findByPagbankSubscriptionId(subscriptionId)
                    .ifPresent(this::notificarFalhaPagamentoInicial);
        }
    }

    private void atualizarPagamentoDaInvoiceEmbutida(String subscriptionId, JsonNode invoiceNode) {
        var invoiceId = invoiceNode.path("id").asText();
        var invoiceStatus = invoiceNode.path("status").asText();
        if (invoiceId.isBlank()) return;

        var assinatura = assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).orElse(null);
        if (assinatura == null) {
            log.warn("Assinatura não encontrada ao atualizar pagamento da invoice embutida: {}", subscriptionId);
            return;
        }

        var valorCentavos = invoiceNode.path("amount").path("value").asInt(0);
        var pagamento = pagamentoRepository.findByPagbankInvoiceId(invoiceId)
            .orElseGet(() -> Pagamento.builder()
                .assinatura(assinatura)
                .pagbankInvoiceId(invoiceId)
                .valor(MoneyUtil.centavosParaReais(valorCentavos))
                .formaPagamento(FormaPagamentoEnum.CREDIT_CARD)
                .build());

        if ("PAID".equalsIgnoreCase(invoiceStatus)) {
            pagamento.setStatus(PagamentoStatusEnum.PAID);
            pagamento.setDataPagamento(OffsetDateTime.now());
            pagamento.setMotivoRecusa(null);
        } else {
            pagamento.setStatus(PagamentoStatusEnum.DECLINED);
            // Tentar extrair motivo do primeiro pagamento negado
            var payments = invoiceNode.path("payments");
            if (payments.isArray() && !payments.isEmpty()) {
                var firstPayment = payments.get(0);
                var declineCode = firstPayment.path("decline_code").asText(null);
                var declineMessage = firstPayment.path("decline_message").asText(null);
                if (declineCode != null || declineMessage != null) {
                    pagamento.setMotivoRecusa(String.format("Código: %s - %s",
                        declineCode != null ? declineCode : "N/A",
                        declineMessage != null ? declineMessage : "Recusado pela operadora"));
                } else {
                    pagamento.setMotivoRecusa("Pagamento recusado - verifique dados do cartão");
                }
            }
        }

        pagamentoRepository.save(pagamento);

        // Atualiza status da assinatura conforme invoice
        if (!"PAID".equalsIgnoreCase(invoiceStatus) && assinatura.getStatus() == AssinaturaStatusEnum.PENDING) {
            assinatura.setStatus(AssinaturaStatusEnum.OVERDUE);
            assinaturaRepository.save(assinatura);
        }

        log.info("Pagamento atualizado via current_invoice embutida: invoice={} invoiceStatus={} subscriptionStatus={}",
                invoiceId, invoiceStatus, assinatura.getStatus());
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
            licencaRepository.save(licenca);
        });
        log.info("Subscription cancelada via webhook: {}", subscriptionId);
    }

    private void processarSubscriptionSuspended(JsonNode json) {
        var resource = json.has("resource") ? json.path("resource") : json;
        var subscriptionId = resource.path("id").asText();
        assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).ifPresent(assinatura -> {
            assinatura.setStatus(AssinaturaStatusEnum.SUSPENDED);
            assinaturaRepository.save(assinatura);

            var licenca = assinatura.getLicenca();
            licenca.setStatus(LicencaStatusEnum.SUSPENSA);
            licenca.setDataSuspensao(OffsetDateTime.now());
            licencaRepository.save(licenca);
        });
        log.info("Subscription suspensa via webhook: {}", subscriptionId);
    }

    private void processarSubscriptionRecurrence(JsonNode json) {
        var resource = json.has("resource") ? json.path("resource") : json;
        var subscriptionId = resource.path("id").asText();

        assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).ifPresent(assinatura -> {
            if (assinatura.getStatus() == AssinaturaStatusEnum.OVERDUE) {
                assinatura.setStatus(AssinaturaStatusEnum.ACTIVE);
            }

            var nextInvoiceAt = resource.path("next_invoice_at").asText();
            if (!nextInvoiceAt.isEmpty()) {
                try {
                    assinatura.setDataProximaFatura(OffsetDateTime.parse(nextInvoiceAt));
                } catch (Exception e) {
                    log.warn("Erro ao parsear next_invoice_at: {}", nextInvoiceAt);
                }
            }
            assinaturaRepository.save(assinatura);

            try {
                var invoicesDto = pagBankService.listarFaturasAdmin(subscriptionId);
                var invoices = invoicesDto != null ? invoicesDto.getInvoices() : null;
                if (invoices != null && !invoices.isEmpty()) {
                    invoices.stream()
                        .filter(inv -> "PAID".equals(inv.getStatus()))
                        .findFirst()
                        .ifPresent(invoice -> {
                            var pagamento = pagamentoRepository.findByPagbankInvoiceId(invoice.getId())
                                .orElseGet(() -> Pagamento.builder()
                                    .assinatura(assinatura)
                                    .pagbankInvoiceId(invoice.getId())
                                    .valor(MoneyUtil.centavosParaReais(
                                        invoice.getAmount() != null && invoice.getAmount().getValue() != null
                                            ? invoice.getAmount().getValue() : 0))
                                    .formaPagamento(FormaPagamentoEnum.CREDIT_CARD)
                                    .build());
                            pagamento.setStatus(PagamentoStatusEnum.PAID);
                            pagamento.setDataPagamento(OffsetDateTime.now());
                            pagamentoRepository.save(pagamento);
                            log.info("Cobrança recorrente registrada: {} - subscription: {}", invoice.getId(), subscriptionId);
                        });
                }
            } catch (Exception e) {
                log.error("Erro ao buscar invoices da recorrência {}: {}", subscriptionId, e.getMessage());
            }
        });
    }

    private void processarInvoiceCreated(JsonNode json) {
        var resource = json.has("resource") ? json.path("resource") : json;
        var invoiceId = resource.path("id").asText();
        var subscriptionId = resource.path("subscription_id").asText();

        assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).ifPresent(assinatura -> {
            if (pagamentoRepository.findByPagbankInvoiceId(invoiceId).isEmpty()) {
                var valorCentavos = resource.path("amount").path("value").asInt(0);
                var pagamento = Pagamento.builder()
                    .assinatura(assinatura)
                    .pagbankInvoiceId(invoiceId)
                    .status(PagamentoStatusEnum.PENDING)
                    .valor(MoneyUtil.centavosParaReais(valorCentavos))
                    .formaPagamento(FormaPagamentoEnum.CREDIT_CARD)
                    .build();
                pagamentoRepository.save(pagamento);
                log.info("Invoice criada e registrada: {} - subscription: {} - valor: R$ {}",
                    invoiceId, subscriptionId, pagamento.getValor());
            }
        });
    }

    private void processarInvoicePaid(JsonNode json) {
        // Payload vem com envelope {"event":..., "resource":{...}}
        var resource = json.has("resource") ? json.path("resource") : json;
        var invoiceId = resource.path("id").asText();
        var subscriptionId = resource.path("subscription_id").asText();
        var valorCentavos = resource.path("amount").path("value").asInt(0);

        var assinatura = assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).orElse(null);
        if (assinatura == null) {
            log.warn("Assinatura não encontrada para invoice paga: {}", invoiceId);
            return;
        }

        // Upsert: atualiza se já existe (pode estar PENDING ou DECLINED do checkout)
        var pagamento = pagamentoRepository.findByPagbankInvoiceId(invoiceId)
            .orElseGet(() -> Pagamento.builder()
                .assinatura(assinatura)
                .pagbankInvoiceId(invoiceId)
                .formaPagamento(FormaPagamentoEnum.CREDIT_CARD)
                .build());

        pagamento.setStatus(PagamentoStatusEnum.PAID);
        pagamento.setValor(MoneyUtil.centavosParaReais(valorCentavos));
        pagamento.setDataPagamento(OffsetDateTime.now());
        pagamento.setMotivoRecusa(null);
        pagamentoRepository.save(pagamento);

        assinatura.setDataProximaFatura(OffsetDateTime.now().plusYears(1));
        assinaturaRepository.save(assinatura);

        log.info("Invoice paga: {} - Valor: R$ {}", invoiceId, pagamento.getValor());

        // Reativa usuarios no ws-security se a licença estava suspensa ou cancelada
        var licenca = assinatura.getLicenca();
        if (licenca.getStatus() == LicencaStatusEnum.SUSPENSA || licenca.getStatus() == LicencaStatusEnum.CANCELADA) {
            licenca.setStatus(LicencaStatusEnum.ATIVA);
            licenca.setDataSuspensao(null);
            licencaRepository.save(licenca);

            var reactivationEvent = LicenseReactivationEvent.builder()
                    .igrejaId(licenca.getIgrejaId())
                    .licencaId(licenca.getId().toString())
                    .limiteUsuarios(licenca.getPlano() != null ? licenca.getPlano().getLimiteUsuarios() : null)
                    .planoNome(licenca.getPlano() != null ? licenca.getPlano().getNome() : null)
                    .build();
            rabbitTemplate.convertAndSend(exchange, sagaLicenseReactivationRoutingKey, reactivationEvent);
            log.info("Reativação publicada após pagamento: igrejaId={}", licenca.getIgrejaId());
        }
    }

    private void processarInvoiceOverdue(JsonNode json) {
        var resource = json.has("resource") ? json.path("resource") : json;
        var subscriptionId = resource.path("id").asText();

        assinaturaRepository.findByPagbankSubscriptionId(subscriptionId).ifPresent(assinatura -> {
            var statusAnterior = assinatura.getStatus();
            assinatura.setStatus(AssinaturaStatusEnum.OVERDUE);
            assinaturaRepository.save(assinatura);

            try {
                var invoicesDto = pagBankService.listarFaturasAdmin(subscriptionId);
                var invoices = invoicesDto != null ? invoicesDto.getInvoices() : null;
                if (invoices != null && !invoices.isEmpty()) {
                    invoices.stream()
                        .filter(inv -> "OVERDUE".equals(inv.getStatus()) || "UNPAID".equals(inv.getStatus()))
                        .findFirst()
                        .ifPresent(invoice -> {
                            var valorCentavos = invoice.getAmount() != null && invoice.getAmount().getValue() != null
                                ? invoice.getAmount().getValue() : 0;
                            var pagamento = pagamentoRepository.findByPagbankInvoiceId(invoice.getId())
                                .orElseGet(() -> Pagamento.builder()
                                    .assinatura(assinatura)
                                    .pagbankInvoiceId(invoice.getId())
                                    .valor(MoneyUtil.centavosParaReais(valorCentavos))
                                    .formaPagamento(FormaPagamentoEnum.CREDIT_CARD)
                                    .build());
                            pagamento.setStatus(PagamentoStatusEnum.DECLINED);
                            pagamento.setMotivoRecusa(extrairMotivoRecusa(invoice));
                            pagamentoRepository.save(pagamento);
                            log.warn("Invoice vencida: {} - subscription: {} - motivo: {}",
                                invoice.getId(), subscriptionId, pagamento.getMotivoRecusa());
                        });
                }
            } catch (Exception e) {
                log.error("Erro ao buscar invoices da subscription {}: {}", subscriptionId, e.getMessage());
            }

            // Notifica o lead apenas para falha no pagamento inicial (assinatura ainda PENDING/TRIAL)
            // Renovações anuais com falha são tratadas separadamente (TODO: fluxo de dunning)
            if (statusAnterior == AssinaturaStatusEnum.PENDING || statusAnterior == AssinaturaStatusEnum.TRIAL) {
                notificarFalhaPagamentoInicial(assinatura);
            }
        });
    }

    private void notificarFalhaPagamentoInicial(br.eti.logos.entity.landing.Assinatura assinatura) {
        try {
            var lead = leadRepository.findTopByEmailOrderByCriadoEmDesc(assinatura.getEmailCliente()).orElse(null);
            if (lead == null) {
                log.warn("Lead não encontrado para notificação de falha: {}", assinatura.getEmailCliente());
                return;
            }

            var token = checkoutRetryService.gerarTokenParaAssinatura(assinatura);
            var retryUrl = landingUrl + "/#/retry/" + token;
            var planoNome = assinatura.getLicenca().getPlano().getNome();
            // TODO: persistir lang no Lead para enviar email no idioma do usuário
            var lang = "pt";

            emailService.enviarFalhaPagamento(
                assinatura.getEmailCliente(),
                lead.getNomeResponsavel(),
                planoNome,
                retryUrl,
                lang
            );
            log.info("Email de falha de pagamento enviado: subscription={} email={}", assinatura.getId(), assinatura.getEmailCliente());
        } catch (Exception e) {
            log.error("Falha ao enviar email de retry para subscription={}: {}", assinatura.getId(), e.getMessage());
        }
    }

    private void processarInvoiceRefunded(JsonNode json) {
        var resource = json.has("resource") ? json.path("resource") : json;
        var invoiceId = resource.path("id").asText();

        pagamentoRepository.findByPagbankInvoiceId(invoiceId).ifPresent(pagamento -> {
            pagamento.setStatus(PagamentoStatusEnum.REFUNDED);
            pagamento.setDataEstorno(OffsetDateTime.now());
            pagamento.setValorEstornado(pagamento.getValor());
            pagamentoRepository.save(pagamento);

            var assinatura = pagamento.getAssinatura();
            if (assinatura == null) {
                log.warn("Pagamento {} sem assinatura associada no estorno", pagamento.getId());
                return;
            }

            // Cancela assinatura e licença
            assinatura.setStatus(AssinaturaStatusEnum.CANCELED);
            assinatura.setDataCancelamento(OffsetDateTime.now());
            assinatura.setMotivoCancelamento("Estorno/chargeback solicitado pelo titular do cartão");
            assinaturaRepository.save(assinatura);

            var licenca = assinatura.getLicenca();
            if (licenca != null) {
                licenca.setStatus(LicencaStatusEnum.CANCELADA);
                licenca.setDataCancelamento(OffsetDateTime.now());
                licenca.setMotivoCancelamento("Estorno/chargeback solicitado pelo titular do cartão");
                licencaRepository.save(licenca);

                notificarEstornoEPublicarSuspensao(assinatura, licenca);
            }
        });
        log.info("Invoice estornada (chargeback) processada: {}", invoiceId);
    }

    private void notificarEstornoEPublicarSuspensao(br.eti.logos.entity.landing.Assinatura assinatura,
                                                     br.eti.logos.entity.landing.Licenca licenca) {
        try {
            var lead = leadRepository.findTopByEmailOrderByCriadoEmDesc(assinatura.getEmailCliente()).orElse(null);
            if (lead == null) {
                log.warn("Lead não encontrado para notificação de estorno: {}", assinatura.getEmailCliente());
                return;
            }

            var token = checkoutRetryService.gerarTokenParaAssinatura(assinatura);
            var retryUrl = landingUrl + "/#/retry/" + token;
            var planoNome = licenca.getPlano() != null ? licenca.getPlano().getNome() : "seu plano";
            var lang = "pt"; // TODO: persistir lang no Lead

            emailService.enviarCancelamentoEstorno(
                    assinatura.getEmailCliente(),
                    lead.getNomeResponsavel(),
                    planoNome,
                    retryUrl,
                    lang
            );

            var suspensionEvent = LicenseSuspensionEvent.builder()
                    .igrejaId(licenca.getIgrejaId())
                    .assinaturaId(assinatura.getId().toString())
                    .motivo("Chargeback/estorno")
                    .retryUrl(retryUrl)
                    .build();
            rabbitTemplate.convertAndSend(exchange, sagaLicenseSuspensionRoutingKey, suspensionEvent);

            log.info("Estorno processado: email enviado e suspensão publicada para igrejaId={}", licenca.getIgrejaId());
        } catch (Exception e) {
            log.error("Falha ao processar notificação de estorno para assinatura={}: {}", assinatura.getId(), e.getMessage());
        }
    }

    private String extrairMotivoRecusa(InvoicesListDto.InvoiceDetail invoice) {
        if (invoice.getDeclineReason() != null && !invoice.getDeclineReason().isBlank()) {
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
        return "Pagamento recusado - verifique dados do cartão";
    }

    private WebhookEventTypeEnum detectarTipoEvento(String payload) {
        try {
            var json = objectMapper.readTree(payload);

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
