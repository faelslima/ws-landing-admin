package br.eti.logos.controller.admin;

import br.eti.logos.dto.request.RefundRequestDto;
import br.eti.logos.dto.response.PagamentoResponseDto;
import br.eti.logos.enums.PagamentoStatusEnum;
import br.eti.logos.service.pagamento.PagamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/pagamentos")
@RequiredArgsConstructor
public class AdminPagamentoApi {

    private final PagamentoService pagamentoService;

    @GetMapping

    public ResponseEntity<Page<PagamentoResponseDto>> listar(
            @RequestParam(required = false) PagamentoStatusEnum status,
            Pageable pageable) {
        return ResponseEntity.ok(pagamentoService.listar(status, pageable));
    }

    @GetMapping("/igreja/{igrejaId}")

    public ResponseEntity<Page<PagamentoResponseDto>> listarPorIgreja(
            @PathVariable UUID igrejaId, Pageable pageable) {
        return ResponseEntity.ok(pagamentoService.listarPorIgreja(igrejaId, pageable));
    }

    @GetMapping("/{id}")

    public ResponseEntity<PagamentoResponseDto> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(pagamentoService.buscarPorId(id));
    }

    @PostMapping("/estorno")

    public ResponseEntity<Void> estornar(@RequestBody @Valid RefundRequestDto request) {
        pagamentoService.estornar(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cancelar")

    public ResponseEntity<Void> cancelar(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        pagamentoService.cancelar(id, body.get("motivo"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/retentar")

    public ResponseEntity<Void> retentar(@PathVariable UUID id) {
        pagamentoService.retentarCobranca(id);
        return ResponseEntity.ok().build();
    }
}
