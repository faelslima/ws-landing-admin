package br.eti.logos.service.licenca.impl;

import br.eti.logos.core.util.DateTimeUtil;
import br.eti.logos.dto.response.LicencaResponseDto;
import br.eti.logos.entity.landing.Licenca;
import br.eti.logos.enums.AssinaturaStatusEnum;
import br.eti.logos.enums.LicencaStatusEnum;
import br.eti.logos.repository.*;
import br.eti.logos.service.licenca.LicencaService;
import br.eti.logos.service.pagbank.PagBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicencaServiceImpl implements LicencaService {

    private final LicencaRepository licencaRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final IgrejaRepository igrejaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagBankService pagBankService;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "licencas", key = "#status != null ? #status.name() + '_' + #pageable.pageNumber : 'all_' + #pageable.pageNumber")
    public Page<LicencaResponseDto> listar(LicencaStatusEnum status, Pageable pageable) {
        log.debug("Buscando licenças com status={}, page={} (cache miss)", status, pageable.getPageNumber());
        Page<Licenca> page;
        if (status != null) {
            page = licencaRepository.findAllByStatus(status, pageable);
        } else {
            page = licencaRepository.findAll(pageable);
        }
        return page.map(this::toDto);
    }

    @Override
    @Cacheable(value = "licencas", key = "'igreja_' + #igrejaId")
    public LicencaResponseDto buscarPorIgreja(String igrejaId) {
        log.debug("Buscando licença por igreja={} (cache miss)", igrejaId);
        var licenca = licencaRepository.findByIgrejaId(igrejaId)
                .orElseThrow(() -> new IllegalArgumentException("Licença não encontrada para igreja: " + igrejaId));
        return toDto(licenca);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"licencas", "dashboard"}, allEntries = true)
    public void suspender(UUID licencaId, String motivo) {
        var licenca = findById(licencaId);
        licenca.setStatus(LicencaStatusEnum.SUSPENSA);
        licenca.setDataSuspensao(OffsetDateTime.now());
        licencaRepository.save(licenca);

        assinaturaRepository.findByLicencaId(licencaId).ifPresent(assinatura -> {
            if (assinatura.getPagbankSubscriptionId() != null) {
                pagBankService.suspenderAssinatura(assinatura.getPagbankSubscriptionId());
            }
            assinatura.setStatus(AssinaturaStatusEnum.SUSPENDED);
            assinaturaRepository.save(assinatura);
        });

        log.info("Licença suspensa: {}", licencaId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"licencas", "dashboard"}, allEntries = true)
    public void reativar(UUID licencaId) {
        var licenca = findById(licencaId);
        licenca.setStatus(LicencaStatusEnum.ATIVA);
        licenca.setDataSuspensao(null);
        licencaRepository.save(licenca);

        assinaturaRepository.findByLicencaId(licencaId).ifPresent(assinatura -> {
            if (assinatura.getPagbankSubscriptionId() != null) {
                pagBankService.reativarAssinatura(assinatura.getPagbankSubscriptionId());
            }
            assinatura.setStatus(AssinaturaStatusEnum.ACTIVE);
            assinaturaRepository.save(assinatura);
        });

        // Reativar igreja
        igrejaRepository.findById(licenca.getIgrejaId()).ifPresent(igreja -> {
            igreja.setAtivo(true);
            igrejaRepository.save(igreja);
        });

        log.info("Licença reativada: {}", licencaId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"licencas", "dashboard"}, allEntries = true)
    public void cancelar(UUID licencaId, String motivo) {
        var licenca = findById(licencaId);
        licenca.setStatus(LicencaStatusEnum.CANCELADA);
        licenca.setDataCancelamento(OffsetDateTime.now());
        licenca.setMotivoCancelamento(motivo);
        licencaRepository.save(licenca);

        assinaturaRepository.findByLicencaId(licencaId).ifPresent(assinatura -> {
            if (assinatura.getPagbankSubscriptionId() != null) {
                pagBankService.cancelarAssinatura(assinatura.getPagbankSubscriptionId());
            }
            assinatura.setStatus(AssinaturaStatusEnum.CANCELED);
            assinatura.setDataCancelamento(OffsetDateTime.now());
            assinatura.setMotivoCancelamento(motivo);
            assinaturaRepository.save(assinatura);
        });

        log.info("Licença cancelada: {} - Motivo: {}", licencaId, motivo);
    }

    @Override
    @Transactional
    @CacheEvict(value = "licencas", allEntries = true)
    public void inativarIgreja(String igrejaId, String motivo) {
        igrejaRepository.findById(igrejaId).ifPresent(igreja -> {
            igreja.setAtivo(false);
            igrejaRepository.save(igreja);
        });

        licencaRepository.findByIgrejaId(igrejaId).ifPresent(licenca -> {
            cancelar(licenca.getId(), motivo);
        });

        log.info("Igreja inativada: {} - Motivo: {}", igrejaId, motivo);
    }

    @Override
    @Transactional
    public void verificarLicencasExpiradas() {
        var expiradas = licencaRepository.findExpiradas();
        for (var licenca : expiradas) {
            licenca.setStatus(LicencaStatusEnum.EXPIRADA);
            licencaRepository.save(licenca);

            igrejaRepository.findById(licenca.getIgrejaId()).ifPresent(igreja -> {
                igreja.setAtivo(false);
                igrejaRepository.save(igreja);
            });
        }
        if (!expiradas.isEmpty()) {
            log.info("Licenças expiradas processadas: {}", expiradas.size());
        }
    }

    private Licenca findById(UUID id) {
        return licencaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Licença não encontrada: " + id));
    }

    private LicencaResponseDto toDto(Licenca licenca) {
        var usuariosAtivos = usuarioRepository.countUsuariosAtivosByIgreja(licenca.getIgrejaId());
        var limite = licenca.getPlano().getLimiteUsuarios();
        var percentual = limite > 0 ? (int) ((usuariosAtivos * 100) / limite) : 0;

        var igreja = igrejaRepository.findById(licenca.getIgrejaId()).orElse(null);

        // dataProximaCobranca vem da assinatura, não da licença
        var assinatura = assinaturaRepository.findByLicencaId(licenca.getId()).orElse(null);
        var dataProximaCobranca = assinatura != null ? assinatura.getDataProximaFatura() : null;

        return LicencaResponseDto.builder()
                .id(licenca.getId())
                .igrejaId(licenca.getIgrejaId())
                .nomeIgreja(igreja != null ? igreja.getRazaoSocial() : "N/A")
                .planoNome(licenca.getPlano().getNome())
                .status(licenca.getStatus())
                .limiteUsuarios(limite)
                .usuariosAtivos(usuariosAtivos.intValue())
                .percentualUso(percentual)
                .dataInicio(DateTimeUtil.toIsoString(licenca.getDataInicio()))
                .dataExpiracao(DateTimeUtil.toIsoString(licenca.getDataExpiracao()))
                .dataProximaCobranca(DateTimeUtil.toIsoString(dataProximaCobranca))
                .build();
    }
}
