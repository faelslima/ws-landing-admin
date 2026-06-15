package br.eti.logos.controller.admin;

import br.eti.logos.dto.response.AssinaturaResponseDto;
import br.eti.logos.enums.AssinaturaStatusEnum;
import br.eti.logos.service.assinatura.AssinaturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/assinaturas")
@RequiredArgsConstructor
public class AdminAssinaturaApi {

    private final AssinaturaService assinaturaService;

    @GetMapping
    public ResponseEntity<Page<AssinaturaResponseDto>> listar(
            @RequestParam(required = false) AssinaturaStatusEnum status,
            Pageable pageable) {
        return ResponseEntity.ok(assinaturaService.listar(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssinaturaResponseDto> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(assinaturaService.buscarPorId(id));
    }

    @GetMapping("/pagbank/{pagbankSubscriptionId}")
    public ResponseEntity<AssinaturaResponseDto> buscarPorPagbankId(@PathVariable String pagbankSubscriptionId) {
        return ResponseEntity.ok(assinaturaService.buscarPorPagbankId(pagbankSubscriptionId));
    }
}
