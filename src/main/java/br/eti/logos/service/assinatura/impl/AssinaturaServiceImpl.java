package br.eti.logos.service.assinatura.impl;

import br.eti.logos.core.util.DateTimeUtil;
import br.eti.logos.dto.response.AssinaturaResponseDto;
import br.eti.logos.enums.AssinaturaStatusEnum;
import br.eti.logos.enums.LicencaStatusEnum;
import br.eti.logos.repository.AssinaturaRepository;
import br.eti.logos.repository.IgrejaRepository;
import br.eti.logos.repository.LicencaRepository;
import br.eti.logos.service.assinatura.AssinaturaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssinaturaServiceImpl implements AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final LicencaRepository licencaRepository;
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

    @Override
    @Transactional
    @CacheEvict(value = {"licencas", "dashboard"}, allEntries = true)
    public void sincronizarStatus(String pagbankSubscriptionId,
                                  AssinaturaStatusEnum statusAssinatura,
                                  LicencaStatusEnum statusLicenca,
                                  String motivo) {
        assinaturaRepository.findByPagbankSubscriptionId(pagbankSubscriptionId).ifPresentOrElse(assinatura -> {
            assinatura.setStatus(statusAssinatura);
            if (statusAssinatura == AssinaturaStatusEnum.CANCELED) {
                assinatura.setDataCancelamento(OffsetDateTime.now());
                assinatura.setMotivoCancelamento(motivo);
            }
            assinaturaRepository.save(assinatura);

            licencaRepository.findById(assinatura.getLicenca().getId()).ifPresent(licenca -> {
                licenca.setStatus(statusLicenca);
                if (statusLicenca == LicencaStatusEnum.CANCELADA) {
                    licenca.setDataCancelamento(OffsetDateTime.now());
                    licenca.setMotivoCancelamento(motivo);
                } else if (statusLicenca == LicencaStatusEnum.SUSPENSA) {
                    licenca.setDataSuspensao(OffsetDateTime.now());
                } else if (statusLicenca == LicencaStatusEnum.ATIVA) {
                    licenca.setDataSuspensao(null);
                }
                licencaRepository.save(licenca);
            });

            log.info("Banco sincronizado: subscription={} assinatura={} licenca={}",
                    pagbankSubscriptionId, statusAssinatura, statusLicenca);
        }, () -> log.warn("Assinatura não encontrada no banco para sincronização: {}", pagbankSubscriptionId));
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
