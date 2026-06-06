package br.eti.logos.service.pagbank.impl;

import br.eti.logos.dto.pagbank.*;
import br.eti.logos.feign.PagBankOrdersFeign;
import br.eti.logos.feign.PagBankSubscriptionsFeign;
import br.eti.logos.service.pagbank.PagBankService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagBankServiceImpl implements PagBankService {

    private final PagBankSubscriptionsFeign subscriptionsFeign;
    private final PagBankOrdersFeign ordersFeign;
    private final ObjectMapper objectMapper;

    @Value("${pagbank.token}")
    private String pagbankToken;

    private String bearerToken() {
        return "Bearer " + pagbankToken;
    }

    // ========================================================================
    // PLANS
    // ========================================================================

    @Override
    public PagBankPlanDto criarPlano(PagBankPlanDto planDto) {
        log.info("Criando plano no PagBank: {}", planDto.getName());
        var response = subscriptionsFeign.criarPlano(bearerToken(), planDto);
        return response.getBody();
    }

    @Override
    public PagBankPlanDto consultarPlano(String planId) {
        var response = subscriptionsFeign.consultarPlano(bearerToken(), planId);
        return response.getBody();
    }

    @Override
    public PagBankSubscriptionDto criarAssinatura(PagBankSubscriptionDto subscriptionDto) {
        log.info("Criando assinatura no PagBank para plano: {}", subscriptionDto.getPlan().getId());
        var response = subscriptionsFeign.criarAssinatura(bearerToken(), subscriptionDto);
        return response.getBody();
    }

    @Override
    public PagBankSubscriptionDto consultarAssinatura(String subscriptionId) {
        var response = subscriptionsFeign.consultarAssinatura(bearerToken(), subscriptionId);
        return response.getBody();
    }

    @Override
    public void cancelarAssinatura(String subscriptionId) {
        log.info("Cancelando assinatura no PagBank: {}", subscriptionId);
        subscriptionsFeign.cancelarAssinatura(bearerToken(), subscriptionId);
    }

    @Override
    public void suspenderAssinatura(String subscriptionId) {
        log.info("Suspendendo assinatura no PagBank: {}", subscriptionId);
        subscriptionsFeign.suspenderAssinatura(bearerToken(), subscriptionId);
    }

    @Override
    public void reativarAssinatura(String subscriptionId) {
        log.info("Reativando assinatura no PagBank: {}", subscriptionId);
        subscriptionsFeign.reativarAssinatura(bearerToken(), subscriptionId);
    }

    @Override
    public List<PagBankInvoiceDto> listarFaturas(String subscriptionId) {
        var response = subscriptionsFeign.listarFaturas(bearerToken(), subscriptionId);
        return response.getBody();
    }

    @Override
    public InvoicesListDto listarFaturasAdmin(String subscriptionId) {
        log.debug("Listando faturas da assinatura {} no PagBank", subscriptionId);
        return subscriptionsFeign.listarFaturasAdmin(bearerToken(), subscriptionId);
    }

    @Override
    public void retentarFatura(String invoiceId) {
        log.info("Retentando fatura no PagBank: {}", invoiceId);
        subscriptionsFeign.retentarFatura(bearerToken(), invoiceId);
    }

    // ========================================================================
    // SUBSCRIPTIONS (novos métodos para AdminPagSeguroApi)
    // ========================================================================

    @Override
    public SubscriptionsListDto listarAssinaturas(
            String searchQuery,
            String status,
            String paymentMethodType,
            String createdAtStart,
            String createdAtEnd,
            String planId,
            String customerId,
            String referenceId,
            Integer offset,
            Integer limit) {
        log.debug("Listando assinaturas no PagBank - status: {}, payment_method: {}, search: {}, offset: {}, limit: {}",
                status, paymentMethodType, searchQuery, offset, limit);
        return subscriptionsFeign.listarAssinaturas(
                bearerToken(),
                searchQuery,
                status,
                paymentMethodType,
                createdAtStart,
                createdAtEnd,
                planId,
                customerId,
                referenceId,
                offset,
                limit
        );
    }

    @Override
    public PagBankSubscriptionResponseDto buscarAssinaturaPorId(String subscriptionId) {
        log.debug("Buscando assinatura {} no PagBank", subscriptionId);
        return subscriptionsFeign.buscarAssinaturaPorId(bearerToken(), subscriptionId);
    }

    @Override
    public PagBankSubscriptionResponseDto suspenderAssinaturaAdmin(String subscriptionId) {
        log.info("Suspendendo assinatura {} no PagBank", subscriptionId);
        return subscriptionsFeign.suspenderAssinaturaAdmin(bearerToken(), subscriptionId);
    }

    @Override
    public PagBankSubscriptionResponseDto reativarAssinaturaAdmin(String subscriptionId) {
        log.info("Reativando assinatura {} no PagBank", subscriptionId);
        return subscriptionsFeign.reativarAssinaturaAdmin(bearerToken(), subscriptionId);
    }

    @Override
    public PagBankSubscriptionResponseDto cancelarAssinaturaAdmin(String subscriptionId) {
        log.warn("Cancelando assinatura {} no PagBank (IRREVERSÍVEL)", subscriptionId);
        return subscriptionsFeign.cancelarAssinaturaAdmin(bearerToken(), subscriptionId);
    }

    // ========================================================================
    // ORDERS
    // ========================================================================

    @Override
    public void cancelarCobranca(String chargeId, Integer valorCentavos) {
        log.info("Cancelando cobrança no PagBank: {} valor: {}", chargeId, valorCentavos);
        try {
            String body = objectMapper.writeValueAsString(Map.of("amount", Map.of("value", valorCentavos)));
            ordersFeign.cancelarCobranca(bearerToken(), chargeId, body);
        } catch (Exception e) {
            log.error("Erro ao cancelar cobrança {}: {}", chargeId, e.getMessage());
            throw new RuntimeException("Falha ao cancelar cobrança no PagBank", e);
        }
    }

    // ========================================================================
    // WEBHOOKS
    // ========================================================================

    @Override
    public PagBankNotificationPreferencesDto consultarPreferenciasNotificacao() {
        log.info("Consultando preferências de notificação no PagBank");
        var response = subscriptionsFeign.consultarPreferenciasNotificacao(bearerToken());
        return response.getBody();
    }

    @Override
    public void atualizarPreferenciasNotificacao(PagBankNotificationPreferencesDto preferences) {
        log.info("Atualizando preferências de notificação no PagBank: {} URLs", preferences.getUrls() != null ? preferences.getUrls().size() : 0);
        subscriptionsFeign.atualizarPreferenciasNotificacao(bearerToken(), preferences);
    }
}
