package br.eti.logos.service.assinatura.impl;

import br.eti.logos.core.util.DateTimeUtil;
import br.eti.logos.dto.response.AssinaturaResponseDto;
import br.eti.logos.enums.AssinaturaStatusEnum;
import br.eti.logos.repository.AssinaturaRepository;
import br.eti.logos.repository.IgrejaRepository;
import br.eti.logos.service.assinatura.AssinaturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssinaturaServiceImpl implements AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final IgrejaRepository igrejaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AssinaturaResponseDto> listar(AssinaturaStatusEnum status, Pageable pageable) {
        var page = status != null
                ? assinaturaRepository.findAllByStatus(status, pageable)
                : assinaturaRepository.findAll(pageable);
        return page.map(a -> {
            var licenca = a.getLicenca();
            var nomeIgreja = igrejaRepository.findById(licenca.getIgrejaId())
                    .map(ig -> ig.getRazaoSocial())
                    .orElse("N/A");
            return toDto(a, licenca.getPlano().getNome(), nomeIgreja);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AssinaturaResponseDto buscarPorId(UUID id) {
        var a = assinaturaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada: " + id));
        var licenca = a.getLicenca();
        var nomeIgreja = igrejaRepository.findById(licenca.getIgrejaId())
                .map(ig -> ig.getRazaoSocial())
                .orElse("N/A");
        return toDto(a, licenca.getPlano().getNome(), nomeIgreja);
    }

    @Override
    @Transactional(readOnly = true)
    public AssinaturaResponseDto buscarPorPagbankId(String pagbankSubscriptionId) {
        var a = assinaturaRepository.findByPagbankSubscriptionId(pagbankSubscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada: " + pagbankSubscriptionId));
        var licenca = a.getLicenca();
        var nomeIgreja = igrejaRepository.findById(licenca.getIgrejaId())
                .map(ig -> ig.getRazaoSocial())
                .orElse("N/A");
        return toDto(a, licenca.getPlano().getNome(), nomeIgreja);
    }

    private AssinaturaResponseDto toDto(br.eti.logos.entity.landing.Assinatura a,
                                        String planoNome, String nomeIgreja) {
        return AssinaturaResponseDto.builder()
                .id(a.getId())
                .pagbankSubscriptionId(a.getPagbankSubscriptionId())
                .nomeIgreja(nomeIgreja)
                .planoNome(planoNome)
                .emailCliente(a.getEmailCliente())
                .valorAnual(a.getValorAnual())
                .status(a.getStatus())
                .dataCriacao(DateTimeUtil.toIsoString(a.getCriadoEm()))
                .dataProximaFatura(DateTimeUtil.toIsoString(a.getDataProximaFatura()))
                .dataCancelamento(DateTimeUtil.toIsoString(a.getDataCancelamento()))
                .build();
    }
}
