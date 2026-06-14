package br.eti.logos.controller.admin;

import br.eti.logos.core.util.DateTimeUtil;
import br.eti.logos.dto.response.AssinaturaResponseDto;
import br.eti.logos.entity.landing.Assinatura;
import br.eti.logos.enums.AssinaturaStatusEnum;
import br.eti.logos.repository.AssinaturaRepository;
import br.eti.logos.repository.IgrejaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/assinaturas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('I12_GESTAO_VENDAS')")
public class AdminAssinaturaApi {

    private final AssinaturaRepository assinaturaRepository;
    private final IgrejaRepository igrejaRepository;

    @GetMapping
    public ResponseEntity<Page<AssinaturaResponseDto>> listar(
            @RequestParam(required = false) AssinaturaStatusEnum status,
            Pageable pageable) {
        Page<Assinatura> page = status != null
                ? assinaturaRepository.findAllByStatus(status, pageable)
                : assinaturaRepository.findAll(pageable);
        return ResponseEntity.ok(page.map(this::toDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssinaturaResponseDto> buscar(@PathVariable UUID id) {
        return assinaturaRepository.findById(id)
                .map(a -> ResponseEntity.ok(toDto(a)))
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada: " + id));
    }

    @GetMapping("/pagbank/{pagbankSubscriptionId}")
    public ResponseEntity<AssinaturaResponseDto> buscarPorPagbankId(@PathVariable String pagbankSubscriptionId) {
        return assinaturaRepository.findByPagbankSubscriptionId(pagbankSubscriptionId)
                .map(a -> ResponseEntity.ok(toDto(a)))
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada: " + pagbankSubscriptionId));
    }

    private AssinaturaResponseDto toDto(Assinatura a) {
        var licenca = a.getLicenca();
        var nomeIgreja = igrejaRepository.findById(licenca.getIgrejaId())
                .map(ig -> ig.getRazaoSocial())
                .orElse("N/A");

        return AssinaturaResponseDto.builder()
                .id(a.getId())
                .pagbankSubscriptionId(a.getPagbankSubscriptionId())
                .nomeIgreja(nomeIgreja)
                .planoNome(licenca.getPlano().getNome())
                .emailCliente(a.getEmailCliente())
                .valorAnual(a.getValorAnual())
                .status(a.getStatus())
                .dataCriacao(DateTimeUtil.toIsoString(a.getCriadoEm()))
                .dataProximaFatura(DateTimeUtil.toIsoString(a.getDataProximaFatura()))
                .dataCancelamento(DateTimeUtil.toIsoString(a.getDataCancelamento()))
                .build();
    }
}
