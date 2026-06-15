package br.eti.logos.controller.admin;

import br.eti.logos.dto.response.LicencaResponseDto;
import br.eti.logos.enums.LicencaStatusEnum;
import br.eti.logos.service.licenca.LicencaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/licencas")
@RequiredArgsConstructor
public class AdminLicencaApi {

    private final LicencaService licencaService;

    @GetMapping

    public ResponseEntity<Page<LicencaResponseDto>> listar(
            @RequestParam(required = false) LicencaStatusEnum status,
            Pageable pageable) {
        return ResponseEntity.ok(licencaService.listar(status, pageable));
    }

    @GetMapping("/igreja/{igrejaId}")

    public ResponseEntity<LicencaResponseDto> buscarPorIgreja(@PathVariable String igrejaId) {
        return ResponseEntity.ok(licencaService.buscarPorIgreja(igrejaId));
    }

    @PutMapping("/{id}/suspender")

    public ResponseEntity<Void> suspender(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        licencaService.suspender(id, body.get("motivo"));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reativar")

    public ResponseEntity<Void> reativar(@PathVariable UUID id) {
        licencaService.reativar(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cancelar")

    public ResponseEntity<Void> cancelar(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        licencaService.cancelar(id, body.get("motivo"));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/igreja/{igrejaId}/inativar")

    public ResponseEntity<Void> inativarIgreja(@PathVariable String igrejaId, @RequestBody Map<String, String> body) {
        licencaService.inativarIgreja(igrejaId, body.get("motivo"));
        return ResponseEntity.ok().build();
    }
}
