package br.eti.logos.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "pagbank-orders", url = "${pagbank.orders.url}")
public interface PagBankOrdersFeign {

    @PostMapping("/charges/{chargeId}/cancel")
    ResponseEntity<String> cancelarCobranca(
            @RequestHeader("Authorization") String token,
            @PathVariable("chargeId") String chargeId,
            @RequestBody String body);

    @GetMapping("/orders/{orderId}")
    ResponseEntity<String> consultarOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable("orderId") String orderId);

    @PostMapping("/public-keys")
    ResponseEntity<String> gerarPublicKey(
            @RequestHeader("Authorization") String token,
            @RequestBody String body);
}
