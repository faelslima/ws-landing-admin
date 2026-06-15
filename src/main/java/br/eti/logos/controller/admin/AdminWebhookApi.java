package br.eti.logos.controller.admin;

import br.eti.logos.dto.pagbank.PagBankNotificationPreferencesDto;
import br.eti.logos.service.pagbank.PagBankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/webhooks")
@RequiredArgsConstructor
public class AdminWebhookApi {

    private final PagBankService pagBankService;

    @GetMapping

    public ResponseEntity<PagBankNotificationPreferencesDto> consultar() {
        return ResponseEntity.ok(pagBankService.consultarPreferenciasNotificacao());
    }

    @PutMapping

    public ResponseEntity<Void> atualizar(@RequestBody PagBankNotificationPreferencesDto preferences) {
        pagBankService.atualizarPreferenciasNotificacao(preferences);
        return ResponseEntity.ok().build();
    }
}
