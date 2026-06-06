package br.eti.logos.controller.admin;

import br.eti.logos.dto.pagbank.CustomersListDto;
import br.eti.logos.dto.pagbank.InvoicesListDto;
import br.eti.logos.dto.pagbank.UpdateBillingInfoRequestDto;
import br.eti.logos.dto.pagbank.UpdateCustomerRequestDto;
import br.eti.logos.dto.pagbank.PagBankSubscriptionResponseDto;
import br.eti.logos.dto.pagbank.SubscriptionsListDto;
import br.eti.logos.service.pagbank.PagBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para gerenciamento direto de recursos do PagBank (Assinaturas API)
 *
 * Arquitetura: Controller → PagBankService → Feign (com token explícito)
 * Não persiste dados localmente. Útil para diagnóstico e gestão avançada.
 *
 * @author Rafael Lima
 * @since 2026-06-05
 */
@Slf4j
@RestController
@RequestMapping("/admin/pagseguro")
@RequiredArgsConstructor
@PreAuthorize("hasRole('I12_GESTAO_VENDAS')")
public class AdminPagSeguroApi {

    private final PagBankService pagBankService;

    // ========================================================================
    // SUBSCRIPTIONS (Assinaturas)
    // ========================================================================

    @GetMapping("/subscriptions")
    public ResponseEntity<SubscriptionsListDto> listarAssinaturas(
            @RequestHeader(value = "q", required = false) String searchQuery,
            @RequestParam(required = false) String status,
            @RequestParam(name = "payment_method_type", required = false) String paymentMethodType,
            @RequestParam(name = "created_at_start", required = false) String createdAtStart,
            @RequestParam(name = "created_at_end", required = false) String createdAtEnd,
            @RequestParam(name = "plan_id", required = false) String planId,
            @RequestParam(name = "customer_id", required = false) String customerId,
            @RequestParam(name = "reference_id", required = false) String referenceId,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "100") Integer limit
    ) {
        log.debug("GET /admin/pagseguro/subscriptions - status={}, payment_method={}, search={}, offset={}, limit={}",
                status, paymentMethodType, searchQuery, offset, limit);

        try {
            SubscriptionsListDto response = pagBankService.listarAssinaturas(
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

            log.info("Listadas {} assinaturas do PagBank (total: {})",
                    response.getSubscriptions().size(), response.getResultSet().getTotal());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao listar assinaturas do PagBank", e);
            throw e;
        }
    }

    @GetMapping("/subscriptions/{subscriptionId}")
    public ResponseEntity<PagBankSubscriptionResponseDto> buscarAssinatura(
            @PathVariable String subscriptionId
    ) {
        log.debug("GET /admin/pagseguro/subscriptions/{}", subscriptionId);

        try {
            PagBankSubscriptionResponseDto response = pagBankService.buscarAssinaturaPorId(subscriptionId);
            log.info("Assinatura {} encontrada - status: {}", subscriptionId, response.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao buscar assinatura {} do PagBank", subscriptionId, e);
            throw e;
        }
    }

    @PutMapping("/subscriptions/{subscriptionId}/suspend")
    public ResponseEntity<PagBankSubscriptionResponseDto> suspenderAssinatura(
            @PathVariable String subscriptionId
    ) {
        log.info("PUT /admin/pagseguro/subscriptions/{}/suspend - Suspendendo assinatura", subscriptionId);

        try {
            PagBankSubscriptionResponseDto response = pagBankService.suspenderAssinaturaAdmin(subscriptionId);
            log.info("Assinatura {} suspensa com sucesso", subscriptionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao suspender assinatura {} no PagBank", subscriptionId, e);
            throw e;
        }
    }

    @PutMapping("/subscriptions/{subscriptionId}/activate")
    public ResponseEntity<PagBankSubscriptionResponseDto> reativarAssinatura(
            @PathVariable String subscriptionId
    ) {
        log.info("PUT /admin/pagseguro/subscriptions/{}/activate - Reativando assinatura", subscriptionId);

        try {
            PagBankSubscriptionResponseDto response = pagBankService.reativarAssinaturaAdmin(subscriptionId);
            log.info("Assinatura {} reativada com sucesso", subscriptionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao reativar assinatura {} no PagBank", subscriptionId, e);
            throw e;
        }
    }

    @PutMapping("/subscriptions/{subscriptionId}/cancel")
    public ResponseEntity<PagBankSubscriptionResponseDto> cancelarAssinatura(
            @PathVariable String subscriptionId
    ) {
        log.warn("PUT /admin/pagseguro/subscriptions/{}/cancel - CANCELANDO ASSINATURA (IRREVERSÍVEL)", subscriptionId);

        try {
            PagBankSubscriptionResponseDto response = pagBankService.cancelarAssinaturaAdmin(subscriptionId);
            log.info("Assinatura {} cancelada com sucesso (status: {})", subscriptionId, response.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao cancelar assinatura {} no PagBank", subscriptionId, e);
            throw e;
        }
    }

    // ========================================================================
    // CUSTOMERS (Assinantes)
    // ========================================================================

    @GetMapping("/customers")
    public ResponseEntity<CustomersListDto> listarClientes(
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "100") Integer limit
    ) {
        log.debug("GET /admin/pagseguro/customers - offset={}, limit={}", offset, limit);
        try {
            CustomersListDto response = pagBankService.listarClientes(offset, limit);
            log.info("Listados {} clientes do PagBank (total: {})",
                    response.getCustomers() != null ? response.getCustomers().size() : 0,
                    response.getResultSet() != null ? response.getResultSet().getTotal() : 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao listar clientes do PagBank", e);
            throw e;
        }
    }

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<CustomersListDto.Customer> buscarCliente(
            @PathVariable String customerId
    ) {
        log.debug("GET /admin/pagseguro/customers/{}", customerId);
        try {
            CustomersListDto.Customer customer = pagBankService.buscarCliente(customerId);
            log.info("Cliente {} encontrado: {}", customerId, customer.getName());
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            log.error("Erro ao buscar cliente {} no PagBank", customerId, e);
            throw e;
        }
    }

    @PutMapping("/customers/{customerId}/billing_info")
    public ResponseEntity<Void> atualizarBillingInfo(
            @PathVariable String customerId,
            @RequestBody java.util.List<UpdateBillingInfoRequestDto> billingInfo
    ) {
        log.info("PUT /admin/pagseguro/customers/{}/billing_info - Atualizando dados de pagamento", customerId);
        try {
            pagBankService.atualizarBillingInfo(customerId, billingInfo);
            log.info("Billing info do cliente {} atualizada com sucesso", customerId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao atualizar billing_info do cliente {} no PagBank", customerId, e);
            throw e;
        }
    }

    @PutMapping("/customers/{customerId}")
    public ResponseEntity<CustomersListDto.Customer> atualizarCliente(
            @PathVariable String customerId,
            @RequestBody UpdateCustomerRequestDto request
    ) {
        log.info("PUT /admin/pagseguro/customers/{} - Atualizando dados cadastrais", customerId);
        try {
            CustomersListDto.Customer updated = pagBankService.atualizarCliente(customerId, request);
            log.info("Dados do cliente {} atualizados com sucesso", customerId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Erro ao atualizar dados do cliente {} no PagBank", customerId, e);
            throw e;
        }
    }

    @GetMapping("/subscriptions/{subscriptionId}/invoices")
    public ResponseEntity<InvoicesListDto> listarFaturas(
            @PathVariable String subscriptionId
    ) {
        log.debug("GET /admin/pagseguro/subscriptions/{}/invoices - Listando faturas", subscriptionId);

        try {
            InvoicesListDto response = pagBankService.listarFaturasAdmin(subscriptionId);
            log.info("Listadas {} faturas da assinatura {}", response.getInvoices().size(), subscriptionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao listar faturas da assinatura {} no PagBank", subscriptionId, e);
            throw e;
        }
    }
}
