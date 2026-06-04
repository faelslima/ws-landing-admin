package br.eti.logos.controller.admin;

import br.eti.logos.dto.request.PlanoCreateRequestDto;
import br.eti.logos.dto.response.PlanoResponseDto;
import br.eti.logos.entity.landing.Plano;
import br.eti.logos.repository.PlanoRepository;
import br.eti.logos.service.plano.PlanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/planos")
@RequiredArgsConstructor
public class AdminPlanoApi {

    private final PlanoRepository planoRepository;
    private final PlanoService planoService;

    @GetMapping
    @PreAuthorize("hasRole('I12_GESTAO_VENDAS')")
    public ResponseEntity<List<PlanoResponseDto>> listar() {
        var planos = planoRepository.findAll().stream().map(this::toDto).toList();
        return ResponseEntity.ok(planos);
    }

    @PostMapping
    @PreAuthorize("hasRole('I12_GESTAO_VENDAS')")
    public ResponseEntity<PlanoResponseDto> criar(@RequestBody @Valid PlanoCreateRequestDto request) {
        var plano = planoService.criar(request);
        return ResponseEntity.ok(toDto(plano));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('I12_GESTAO_VENDAS')")
    public ResponseEntity<PlanoResponseDto> atualizar(@PathVariable UUID id, @RequestBody PlanoCreateRequestDto request) {
        var plano = planoService.atualizar(id, request);
        return ResponseEntity.ok(toDto(plano));
    }

    @PostMapping("/{id}/sincronizar-pagbank")
    @PreAuthorize("hasRole('I12_GESTAO_VENDAS')")
    public ResponseEntity<Void> sincronizarPagBank(@PathVariable UUID id) {
        planoService.sincronizarComPagBank(id);
        return ResponseEntity.ok().build();
    }

    private PlanoResponseDto toDto(Plano p) {
        return PlanoResponseDto.builder()
                .id(p.getId())
                .nome(p.getNome())
                .descricao(p.getDescricao())
                .tier(p.getTier())
                .limiteUsuarios(p.getLimiteUsuarios())
                .valorAnualCentavos(p.getValorAnualCentavos())
                .ativo(p.getAtivo())
                .pagbankPlanId(p.getPagbankPlanId())
                .recursos(p.getRecursos())
                .build();
    }
}
