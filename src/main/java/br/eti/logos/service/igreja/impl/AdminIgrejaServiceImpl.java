package br.eti.logos.service.igreja.impl;

import br.eti.logos.core.util.DateTimeUtil;
import br.eti.logos.core.validation.CnpjValidator;
import br.eti.logos.dto.request.IgrejaManualRequestDto;
import br.eti.logos.dto.response.IgrejaResponseDto;
import br.eti.logos.entity.igreja.Igreja;
import br.eti.logos.entity.landing.Licenca;
import br.eti.logos.repository.IgrejaRepository;
import br.eti.logos.repository.LicencaRepository;
import br.eti.logos.repository.specification.IgrejaSpecification;
import br.eti.logos.service.igreja.AdminIgrejaService;
import br.eti.logos.service.licenca.LicencaService;
import br.eti.logos.service.saga.OnboardingSagaPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminIgrejaServiceImpl implements AdminIgrejaService {

    private final IgrejaRepository igrejaRepository;
    private final LicencaRepository licencaRepository;
    private final LicencaService licencaService;
    private final OnboardingSagaPublisher sagaPublisher;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "igrejas",
            key = "(#termo != null ? #termo : 'all') + '_' + (#ativo != null ? #ativo : 'any') + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<IgrejaResponseDto> listar(String termo, Boolean ativo, Pageable pageable) {
        log.debug("Buscando igrejas termo={} ativo={} page={} (cache miss)", termo, ativo, pageable.getPageNumber());
        return igrejaRepository.findAll(IgrejaSpecification.filtro(termo, ativo), pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public IgrejaResponseDto buscar(String igrejaId) {
        var igreja = igrejaRepository.findById(igrejaId)
                .orElseThrow(() -> new IllegalArgumentException("Igreja não encontrada: " + igrejaId));
        return toDto(igreja);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"igrejas", "licencas", "dashboard"}, allEntries = true)
    public IgrejaResponseDto criar(IgrejaManualRequestDto request) {
        var cnpj = CnpjValidator.strip(request.getCnpj());
        if (cnpj != null && !cnpj.isBlank()) {
            igrejaRepository.findByCnpj(cnpj).ifPresent(existente -> {
                throw new IllegalStateException("Já existe uma igreja cadastrada com este CNPJ");
            });
        }

        var igrejaId = UUID.randomUUID().toString();
        var igreja = Igreja.builder()
                .id(igrejaId)
                .razaoSocial(request.getRazaoSocial())
                .nomeFantasia(request.getNomeFantasia() != null ? request.getNomeFantasia() : request.getRazaoSocial())
                .sigla(request.getSigla())
                .cnpj(cnpj)
                .email(request.getEmail())
                .telefone(request.getTelefone())
                .nomeResponsavel(request.getNomeResponsavel())
                .ativo(true)
                .build();
        igrejaRepository.save(igreja);
        log.info("Igreja criada manualmente: id={} razaoSocial={}", igrejaId, request.getRazaoSocial());

        // Licença opcional: quando presente, cria a licença e dispara a saga de provisionamento
        if (request.getLicenca() != null) {
            licencaService.criarOuAtualizarLicencaManual(igrejaId, request.getLicenca());
        }

        return buscar(igrejaId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"igrejas", "licencas", "dashboard"}, allEntries = true)
    public void inativar(String igrejaId, String motivo) {
        igrejaRepository.findById(igrejaId)
                .orElseThrow(() -> new IllegalArgumentException("Igreja não encontrada: " + igrejaId));
        licencaService.inativarIgreja(igrejaId, motivo);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"igrejas", "licencas", "dashboard"}, allEntries = true)
    public void reativar(String igrejaId) {
        var igreja = igrejaRepository.findById(igrejaId)
                .orElseThrow(() -> new IllegalArgumentException("Igreja não encontrada: " + igrejaId));

        var licenca = licencaRepository.findByIgrejaId(igrejaId).orElse(null);
        if (licenca != null) {
            licencaService.reativar(licenca.getId());
        } else {
            igreja.setAtivo(true);
            igrejaRepository.save(igreja);
            sagaPublisher.publicarReativacao(igrejaId, null, null);
            log.info("Igreja sem licença reativada: {}", igrejaId);
        }
    }

    private IgrejaResponseDto toDto(Igreja igreja) {
        var licenca = licencaRepository.findByIgrejaId(igreja.getId()).orElse(null);
        return build(igreja, licenca);
    }

    private IgrejaResponseDto build(Igreja igreja, Licenca licenca) {
        var builder = IgrejaResponseDto.builder()
                .id(igreja.getId())
                .razaoSocial(igreja.getRazaoSocial())
                .nomeFantasia(igreja.getNomeFantasia())
                .sigla(igreja.getSigla())
                .cnpj(igreja.getCnpj())
                .email(igreja.getEmail())
                .telefone(igreja.getTelefone())
                .nomeResponsavel(igreja.getNomeResponsavel())
                .ativo(igreja.getAtivo());

        if (licenca != null) {
            builder.licencaId(licenca.getId().toString())
                    .planoNome(licenca.getPlano() != null ? licenca.getPlano().getNome() : null)
                    .licencaStatus(licenca.getStatus())
                    .limiteUsuarios(licenca.getPlano() != null ? licenca.getPlano().getLimiteUsuarios() : null)
                    .dataInicio(DateTimeUtil.toIsoString(licenca.getDataInicio()))
                    .dataExpiracao(DateTimeUtil.toIsoString(licenca.getDataExpiracao()))
                    .prazoIndeterminado(licenca.getDataExpiracao() == null);
        }

        return builder.build();
    }
}
