package br.eti.logos.feign;

import br.eti.logos.dto.pagbank.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "pagbank-subscriptions", url = "${pagbank.subscriptions.url}")
public interface PagBankSubscriptionsFeign {

    // Plans
    @PostMapping("/plans")
    ResponseEntity<PagBankPlanDto> criarPlano(
            @RequestHeader("Authorization") String token,
            @RequestBody PagBankPlanDto plan);

    @GetMapping("/plans/{planId}")
    ResponseEntity<PagBankPlanDto> consultarPlano(
            @RequestHeader("Authorization") String token,
            @PathVariable("planId") String planId);

    @GetMapping("/plans")
    ResponseEntity<List<PagBankPlanDto>> listarPlanos(
            @RequestHeader("Authorization") String token);

    // Subscriptions (métodos antigos - usados pelo PagBankServiceImpl)
    @PostMapping("/subscriptions")
    ResponseEntity<PagBankSubscriptionDto> criarAssinatura(
            @RequestHeader("Authorization") String token,
            @RequestBody PagBankSubscriptionDto subscription);

    @GetMapping("/subscriptions/{subscriptionId}")
    ResponseEntity<PagBankSubscriptionDto> consultarAssinatura(
            @RequestHeader("Authorization") String token,
            @PathVariable("subscriptionId") String subscriptionId);

    @PutMapping("/subscriptions/{subscriptionId}/cancel")
    ResponseEntity<Void> cancelarAssinatura(
            @RequestHeader("Authorization") String token,
            @PathVariable("subscriptionId") String subscriptionId);

    @PutMapping("/subscriptions/{subscriptionId}/suspend")
    ResponseEntity<Void> suspenderAssinatura(
            @RequestHeader("Authorization") String token,
            @PathVariable("subscriptionId") String subscriptionId);

    @PutMapping("/subscriptions/{subscriptionId}/activate")
    ResponseEntity<Void> reativarAssinatura(
            @RequestHeader("Authorization") String token,
            @PathVariable("subscriptionId") String subscriptionId);

    // Subscriptions (novos métodos - usados pelo PagBankServiceImpl via AdminPagSeguroApi)
    /**
     * Listar assinaturas com filtros opcionais
     * GET /subscriptions?status=ACTIVE&offset=0&limit=100
     *
     * Filtros disponíveis:
     * - status: ACTIVE, EXPIRED, CANCELED, SUSPENDED, OVERDUE, TRIAL, PENDING, PENDING_ACTION
     * - payment_method_type: BOLETO, CREDIT_CARD
     * - created_at_start: Data início (YYYY-MM-DD)
     * - created_at_end: Data fim (YYYY-MM-DD)
     * - q (header): Busca por nome/email/id do assinante
     */
    @GetMapping("/subscriptions")
    SubscriptionsListDto listarAssinaturas(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "q", required = false) String searchQuery,
            @RequestParam(required = false) String status,
            @RequestParam(name = "payment_method_type", required = false) String paymentMethodType,
            @RequestParam(name = "created_at_start", required = false) String createdAtStart,
            @RequestParam(name = "created_at_end", required = false) String createdAtEnd,
            @RequestParam(name = "plan_id", required = false) String planId,
            @RequestParam(name = "customer_id", required = false) String customerId,
            @RequestParam(name = "reference_id", required = false) String referenceId,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit);

    /**
     * Buscar assinatura por ID (retorna DTO completo)
     * GET /subscriptions/{subscriptionId}
     */
    @GetMapping("/subscriptions/{subscriptionId}")
    PagBankSubscriptionResponseDto buscarAssinaturaPorId(
            @RequestHeader("Authorization") String token,
            @PathVariable("subscriptionId") String subscriptionId);

    /**
     * Cancelar assinatura (retorna subscription atualizada)
     */
    @PutMapping("/subscriptions/{subscriptionId}/cancel")
    PagBankSubscriptionResponseDto cancelarAssinaturaAdmin(
            @RequestHeader("Authorization") String token,
            @PathVariable("subscriptionId") String subscriptionId);

    /**
     * Suspender assinatura (retorna subscription atualizada)
     */
    @PutMapping("/subscriptions/{subscriptionId}/suspend")
    PagBankSubscriptionResponseDto suspenderAssinaturaAdmin(
            @RequestHeader("Authorization") String token,
            @PathVariable("subscriptionId") String subscriptionId);

    /**
     * Reativar assinatura (retorna subscription atualizada)
     */
    @PutMapping("/subscriptions/{subscriptionId}/activate")
    PagBankSubscriptionResponseDto reativarAssinaturaAdmin(
            @RequestHeader("Authorization") String token,
            @PathVariable("subscriptionId") String subscriptionId);

    // Invoices
    @GetMapping("/subscriptions/{subscriptionId}/invoices")
    InvoicesListDto listarFaturasAdmin(
            @RequestHeader("Authorization") String token,
            @PathVariable("subscriptionId") String subscriptionId);

    // Customers
    @GetMapping("/customers")
    CustomersListDto listarClientes(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "q", required = false) String searchQuery,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit);

    @GetMapping("/customers/{customerId}")
    CustomersListDto.Customer buscarCliente(
            @RequestHeader("Authorization") String token,
            @PathVariable("customerId") String customerId);

    @PutMapping("/customers/{customerId}")
    CustomersListDto.Customer atualizarCliente(
            @RequestHeader("Authorization") String token,
            @PathVariable("customerId") String customerId,
            @RequestBody UpdateCustomerRequestDto request);

    @PutMapping("/customers/{customerId}/billing_info")
    void atualizarBillingInfo(
            @RequestHeader("Authorization") String token,
            @PathVariable("customerId") String customerId,
            @RequestBody java.util.List<UpdateBillingInfoRequestDto> billingInfo);

    @PostMapping("/invoices/{invoiceId}/retry")
    ResponseEntity<Void> retentarFatura(
            @RequestHeader("Authorization") String token,
            @PathVariable("invoiceId") String invoiceId);

    // Notification Preferences (Webhooks)
    @GetMapping("/preferences/notifications")
    ResponseEntity<br.eti.logos.dto.pagbank.PagBankNotificationPreferencesDto> consultarPreferenciasNotificacao(
            @RequestHeader("Authorization") String token);

    @PutMapping("/preferences/notifications")
    ResponseEntity<Void> atualizarPreferenciasNotificacao(
            @RequestHeader("Authorization") String token,
            @RequestBody br.eti.logos.dto.pagbank.PagBankNotificationPreferencesDto preferences);
}
