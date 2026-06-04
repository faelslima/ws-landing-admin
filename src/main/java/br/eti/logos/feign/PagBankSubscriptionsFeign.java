package br.eti.logos.feign;

import br.eti.logos.dto.pagbank.PagBankInvoiceDto;
import br.eti.logos.dto.pagbank.PagBankPlanDto;
import br.eti.logos.dto.pagbank.PagBankSubscriptionDto;
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

    // Subscriptions
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

    // Invoices
    @GetMapping("/subscriptions/{subscriptionId}/invoices")
    ResponseEntity<List<PagBankInvoiceDto>> listarFaturas(
            @RequestHeader("Authorization") String token,
            @PathVariable("subscriptionId") String subscriptionId);

    @PostMapping("/invoices/{invoiceId}/retry")
    ResponseEntity<Void> retentarFatura(
            @RequestHeader("Authorization") String token,
            @PathVariable("invoiceId") String invoiceId);
}
