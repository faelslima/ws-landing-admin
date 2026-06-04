package br.eti.logos.controller.admin;

import br.eti.logos.dto.response.LeadResponseDto;
import br.eti.logos.entity.landing.Lead;
import br.eti.logos.enums.LeadStatusEnum;
import br.eti.logos.repository.LeadRepository;
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

    private final LeadRepository leadRepository;

    @GetMapping
    @PreAuthorize("hasRole('I12_GESTAO_VENDAS')")
    public ResponseEntity<Page<LeadResponseDto>> listar(
            @RequestParam(required = false) LeadStatusEnum status,
            Pageable pageable) {
        Page<Lead> page;
        if (status != null) {
            page = leadRepository.findAllByStatus(status, pageable);
        } else {
            page = leadRepository.findAllByOrderByCriadoEmDesc(pageable);
        }
        return ResponseEntity.ok(page.map(this::toDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('I12_GESTAO_VENDAS')")
    public ResponseEntity<LeadResponseDto> buscar(@PathVariable UUID id) {
        var lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado"));
        return ResponseEntity.ok(toDto(lead));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('I12_GESTAO_VENDAS')")
    public ResponseEntity<LeadResponseDto> atualizarStatus(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        var lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado"));
        lead.setStatus(LeadStatusEnum.valueOf(body.get("status")));
        if (body.containsKey("observacao")) {
            lead.setObservacao(body.get("observacao"));
        }
        leadRepository.save(lead);
        return ResponseEntity.ok(toDto(lead));
    }

    private LeadResponseDto toDto(Lead lead) {
        return LeadResponseDto.builder()
                .id(lead.getId())
                .nomeIgreja(lead.getNomeIgreja())
                .nomeResponsavel(lead.getNomeResponsavel())
                .email(lead.getEmail())
                .telefone(lead.getTelefone())
                .cnpj(lead.getCnpj())
                .cidade(lead.getCidade())
                .estado(lead.getEstado())
                .quantidadeMembros(lead.getQuantidadeMembros())
                .observacao(lead.getObservacao())
                .status(lead.getStatus())
                .igrejaIdConvertida(lead.getIgrejaIdConvertida())
                .dataConversao(lead.getDataConversao())
                .criadoEm(lead.getCriadoEm())
                .atualizadoEm(lead.getAtualizadoEm())
                .build();
    }
}
