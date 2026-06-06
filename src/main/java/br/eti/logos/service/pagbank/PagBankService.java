package br.eti.logos.service.pagbank;

import br.eti.logos.dto.pagbank.*;

import java.util.List;

public interface PagBankService {

    // Plans
    PagBankPlanDto criarPlano(PagBankPlanDto planDto);

    PagBankPlanDto consultarPlano(String planId);

    // Subscriptions (métodos antigos - mantidos para compatibilidade)
    PagBankSubscriptionDto criarAssinatura(PagBankSubscriptionDto subscriptionDto);

    PagBankSubscriptionDto consultarAssinatura(String subscriptionId);

    void cancelarAssinatura(String subscriptionId);

    void suspenderAssinatura(String subscriptionId);

    void reativarAssinatura(String subscriptionId);

    // Subscriptions (novos métodos para AdminPagSeguroApi)
    SubscriptionsListDto listarAssinaturas(
            String searchQuery,
            String status,
            String paymentMethodType,
            String createdAtStart,
            String createdAtEnd,
            String planId,
            String customerId,
            String referenceId,
            Integer offset,
            Integer limit);

    PagBankSubscriptionResponseDto buscarAssinaturaPorId(String subscriptionId);

    PagBankSubscriptionResponseDto suspenderAssinaturaAdmin(String subscriptionId);

    PagBankSubscriptionResponseDto reativarAssinaturaAdmin(String subscriptionId);

    PagBankSubscriptionResponseDto cancelarAssinaturaAdmin(String subscriptionId);

    // Invoices (método antigo - usado por webhooks)
    List<PagBankInvoiceDto> listarFaturas(String subscriptionId);

    // Invoices (novo método para AdminPagSeguroApi)
    InvoicesListDto listarFaturasAdmin(String subscriptionId);

    // Customers
    CustomersListDto listarClientes(Integer offset, Integer limit);

    CustomersListDto.Customer buscarCliente(String customerId);

    CustomersListDto.Customer atualizarCliente(String customerId, UpdateCustomerRequestDto request);

    void atualizarBillingInfo(String customerId, java.util.List<UpdateBillingInfoRequestDto> billingInfo);

    void retentarFatura(String invoiceId);

    // Orders
    void cancelarCobranca(String chargeId, Integer valorCentavos);

    // Webhooks
    PagBankNotificationPreferencesDto consultarPreferenciasNotificacao();

    void atualizarPreferenciasNotificacao(PagBankNotificationPreferencesDto preferences);
}
