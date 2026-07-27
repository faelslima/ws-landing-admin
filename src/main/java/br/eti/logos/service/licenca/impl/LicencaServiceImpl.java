package br.eti.logos.service.licenca.impl;

import br.eti.logos.core.util.DateTimeUtil;
import br.eti.logos.dto.request.LicencaManualRequestDto;
import br.eti.logos.dto.response.LicencaResponseDto;
import br.eti.logos.entity.landing.Licenca;
import br.eti.logos.enums.AssinaturaStatusEnum;
import br.eti.logos.enums.LicencaStatusEnum;
import br.eti.logos.entity.landing.Lead;
import br.eti.logos.repository.*;
import br.eti.logos.service.licenca.LicencaService;
import br.eti.logos.service.pagbank.PagBankService;
import br.eti.logos.service.saga.OnboardingSagaPublisher;
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
    private final LeadRepository leadRepository;
    private final PlanoRepository planoRepository;
    private final PagBankService pagBankService;
    private final OnboardingSagaPublisher sagaPublisher;

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
    @CacheEvict(value = {"licencas", "dashboard", "igrejas"}, allEntries = true)
    public LicencaResponseDto criarOuAtualizarLicencaManual(String igrejaId, LicencaManualRequestDto request) {
        var igreja = igrejaRepository.findById(igrejaId)
                .orElseThrow(() -> new IllegalArgumentException("Igreja não encontrada: " + igrejaId));

        var plano = planoRepository.findById(request.getPlanoId())
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado: " + request.getPlanoId()));

        var expiracao = request.isPrazoIndeterminado()
                ? null
                : DateTimeUtil.fromIsoString(request.getDataExpiracao());
        if (!request.isPrazoIndeterminado() && expiracao == null) {
            throw new IllegalArgumentException("Informe a data de expiração ou marque prazo indeterminado");
        }

        var licencaExistente = licencaRepository.findByIgrejaId(igrejaId).orElse(null);
        boolean novaLicenca = licencaExistente == null;

        Licenca licenca;
        if (novaLicenca) {
            licenca = Licenca.builder()
                    .igrejaId(igrejaId)
                    .plano(plano)
                    .status(LicencaStatusEnum.ATIVA)
                    .dataInicio(OffsetDateTime.now())
                    .dataExpiracao(expiracao)
                    .build();
        } else {
            licenca = licencaExistente;
            licenca.setPlano(plano);
            licenca.setDataExpiracao(expiracao);
            licenca.setStatus(LicencaStatusEnum.ATIVA);
            licenca.setDataSuspensao(null);
            licenca.setDataCancelamento(null);
            licenca.setMotivoCancelamento(null);
        }
        licencaRepository.save(licenca);

        // Liberação manual reativa a igreja
        igreja.setAtivo(true);
        igrejaRepository.save(igreja);

        // Primeira liberação → provisiona (cria igreja/usuário/descendências no ws-security).
        // Renovação de licença existente → reativa (igreja já provisionada).
        if (novaLicenca) {
            sagaPublisher.publicarProvisionamento(igreja, plano, licenca.getId(), "pt");
        } else {
            sagaPublisher.publicarReativacao(igrejaId, licenca.getId(), plano);
        }

        log.info("Licença manual {} para igreja {}: plano={} expiracao={}",
                novaLicenca ? "criada" : "atualizada", igrejaId, plano.getNome(),
                expiracao != null ? expiracao : "INDETERMINADA");
        return toDto(licenca);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"licencas", "dashboard", "igrejas"}, allEntries = true)
    public void suspender(UUID licencaId, String motivo) {
        var licenca = findById(licencaId);
        licenca.setStatus(LicencaStatusEnum.SUSPENSA);
        licenca.setDataSuspensao(OffsetDateTime.now());
        licencaRepository.save(licenca);

        var assinatura = assinaturaRepository.findByLicencaId(licencaId).orElse(null);
        if (assinatura != null) {
            if (assinatura.getPagbankSubscriptionId() != null) {
                pagBankService.suspenderAssinatura(assinatura.getPagbankSubscriptionId());
            }
            assinatura.setStatus(AssinaturaStatusEnum.SUSPENDED);
            assinaturaRepository.save(assinatura);
        }

        // Inativa igreja + usuários vinculados no ws-security
        inativarIgrejaLocal(licenca.getIgrejaId());
        sagaPublisher.publicarSuspensao(
                licenca.getIgrejaId(),
                assinatura != null ? assinatura.getId().toString() : null,
                motivo);

        log.info("Licença suspensa: {}", licencaId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"licencas", "dashboard", "igrejas"}, allEntries = true)
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

        // Reativa igreja + usuários vinculados no ws-security
        sagaPublisher.publicarReativacao(licenca.getIgrejaId(), licenca.getId(), licenca.getPlano());

        log.info("Licença reativada: {}", licencaId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"licencas", "dashboard", "igrejas"}, allEntries = true)
    public void cancelar(UUID licencaId, String motivo) {
        var licenca = findById(licencaId);
        licenca.setStatus(LicencaStatusEnum.CANCELADA);
        licenca.setDataCancelamento(OffsetDateTime.now());
        licenca.setMotivoCancelamento(motivo);
        licencaRepository.save(licenca);

        var assinatura = assinaturaRepository.findByLicencaId(licencaId).orElse(null);
        if (assinatura != null) {
            if (assinatura.getPagbankSubscriptionId() != null) {
                pagBankService.cancelarAssinatura(assinatura.getPagbankSubscriptionId());
            }
            assinatura.setStatus(AssinaturaStatusEnum.CANCELED);
            assinatura.setDataCancelamento(OffsetDateTime.now());
            assinatura.setMotivoCancelamento(motivo);
            assinaturaRepository.save(assinatura);
        }

        // Revogar = inativar igreja + usuários vinculados no ws-security
        inativarIgrejaLocal(licenca.getIgrejaId());
        sagaPublisher.publicarSuspensao(
                licenca.getIgrejaId(),
                assinatura != null ? assinatura.getId().toString() : null,
                motivo);

        log.info("Licença cancelada: {} - Motivo: {}", licencaId, motivo);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"licencas", "dashboard", "igrejas"}, allEntries = true)
    public void inativarIgreja(String igrejaId, String motivo) {
        var licenca = licencaRepository.findByIgrejaId(igrejaId).orElse(null);
        if (licenca != null) {
            // cancelar() já inativa a igreja e publica a suspensão da saga
            cancelar(licenca.getId(), motivo);
        } else {
            // Igreja sem licença: inativa localmente e sinaliza o ws-security
            inativarIgrejaLocal(igrejaId);
            sagaPublisher.publicarSuspensao(igrejaId, null, motivo);
        }

        log.info("Igreja inativada: {} - Motivo: {}", igrejaId, motivo);
    }

    private void inativarIgrejaLocal(String igrejaId) {
        igrejaRepository.findById(igrejaId).ifPresent(igreja -> {
            igreja.setAtivo(false);
            igrejaRepository.save(igreja);
        });
    }

    @Override
    @Transactional
    @CacheEvict(value = {"licencas", "dashboard", "igrejas"}, allEntries = true)
    public void verificarLicencasExpiradas() {
        var expiradas = licencaRepository.findExpiradas();
        for (var licenca : expiradas) {
            licenca.setStatus(LicencaStatusEnum.EXPIRADA);
            licencaRepository.save(licenca);

            inativarIgrejaLocal(licenca.getIgrejaId());
            // Expiração também revoga: inativa igreja + usuários vinculados no ws-security
            sagaPublisher.publicarSuspensao(licenca.getIgrejaId(), null, "Licença expirada");
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
        var limite = licenca.getPlano().getLimiteUsuarios();

        var igreja = igrejaRepository.findById(licenca.getIgrejaId()).orElse(null);
        var lead = leadRepository.findTopByIgrejaIdConvertidaOrderByCriadoEmDesc(licenca.getIgrejaId()).orElse(null);

        var nomeIgreja = igreja != null ? igreja.getRazaoSocial()
                : (lead != null ? lead.getNomeIgreja() : "N/A");
        var nomeResponsavel = igreja != null ? igreja.getNomeResponsavel()
                : (lead != null ? lead.getNomeResponsavel() : null);

        // dataProximaCobranca vem da assinatura, não da licença
        var assinatura = assinaturaRepository.findByLicencaId(licenca.getId()).orElse(null);
        var dataProximaCobranca = assinatura != null ? assinatura.getDataProximaFatura() : null;

        return LicencaResponseDto.builder()
                .id(licenca.getId())
                .igrejaId(licenca.getIgrejaId())
                .nomeIgreja(nomeIgreja)
                .nomeResponsavel(nomeResponsavel)
                .planoNome(licenca.getPlano().getNome())
                .status(licenca.getStatus())
                .limiteUsuarios(limite)
                .usuariosAtivos(0)
                .percentualUso(0)
                .dataInicio(DateTimeUtil.toIsoString(licenca.getDataInicio()))
                .dataExpiracao(DateTimeUtil.toIsoString(licenca.getDataExpiracao()))
                .dataProximaCobranca(DateTimeUtil.toIsoString(dataProximaCobranca))
                .build();
    }
}
