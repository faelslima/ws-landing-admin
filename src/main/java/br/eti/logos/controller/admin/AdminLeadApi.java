package br.eti.logos.controller.admin;

import br.eti.logos.dto.response.LeadResponseDto;
import br.eti.logos.enums.LeadStatusEnum;
import br.eti.logos.service.lead.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/leads")
@RequiredArgsConstructor
public class AdminLeadApi {

    private final LeadService leadService;

    @GetMapping
    @PreAuthorize("hasRole('I12_GESTAO_VENDAS')")
    public ResponseEntity<Page<LeadResponseDto>> listar(
            @RequestParam(required = false) LeadStatusEnum status,
            Pageable pageable) {
        return ResponseEntity.ok(leadService.listar(status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('I12_GESTAO_VENDAS')")
    public ResponseEntity<LeadResponseDto> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(leadService.buscarPorId(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('I12_GESTAO_VENDAS')")
    public ResponseEntity<LeadResponseDto> atualizarStatus(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        var novoStatus = LeadStatusEnum.valueOf(body.get("status"));
        var observacao = body.get("observacao");
        return ResponseEntity.ok(leadService.atualizarStatus(id, novoStatus, observacao));
    }
}
