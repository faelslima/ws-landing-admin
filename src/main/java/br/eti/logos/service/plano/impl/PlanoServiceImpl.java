package br.eti.logos.service.plano.impl;

import br.eti.logos.dto.pagbank.PagBankPlanDto;
import br.eti.logos.dto.request.PlanoCreateRequestDto;
import br.eti.logos.dto.response.PlanoResponseDto;
import br.eti.logos.entity.landing.Plano;
import br.eti.logos.repository.PlanoRepository;
import br.eti.logos.service.pagbank.PagBankService;
import br.eti.logos.service.plano.PlanoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanoServiceImpl implements PlanoService {

    private final PlanoRepository planoRepository;
    private final PagBankService pagBankService;

    @Override
    @Cacheable(value = "planos", key = "'todos'")
    public List<PlanoResponseDto> listarTodos() {
        log.debug("Buscando todos os planos no banco (cache miss)");
        return planoRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Cacheable(value = "planos", key = "'ativos'")
    public List<PlanoResponseDto> listarPlanosAtivos() {
        log.debug("Buscando planos ativos no banco (cache miss)");
        return planoRepository.findAllByAtivoTrue().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "planos", allEntries = true)
    public Plano criar(PlanoCreateRequestDto request) {
        log.info("Criando plano: {}", request.getNome());

        var plano = Plano.builder()
                .nome(request.getNome())
                .descricao(request.getDescricao())
                .tier(request.getTier())
                .limiteUsuarios(request.getLimiteUsuarios())
                .valorAnualCentavos(request.getValorAnualCentavos())
                .recursos(request.getRecursos())
                .ativo(request.getAtivo() != null ? request.getAtivo() : true)
                .build();

        plano = planoRepository.save(plano);

        // Sincronizar automaticamente com PagBank
        try {
            sincronizarComPagBank(plano.getId());
            log.info("Plano {} sincronizado com PagBank", plano.getNome());
        } catch (Exception e) {
            log.error("Erro ao sincronizar plano {} com PagBank: {}", plano.getNome(), e.getMessage());
            // Não falha a criação se PagBank estiver fora
        }

        return plano;
    }

    @Override
    @Transactional
    @CacheEvict(value = "planos", allEntries = true)
    public Plano atualizar(UUID id, PlanoCreateRequestDto request) {
        log.info("Atualizando plano: {}", id);

        var plano = planoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado"));

        boolean valorAlterado = false;

        if (request.getNome() != null) plano.setNome(request.getNome());
        if (request.getDescricao() != null) plano.setDescricao(request.getDescricao());
        if (request.getLimiteUsuarios() != null) plano.setLimiteUsuarios(request.getLimiteUsuarios());
        if (request.getAtivo() != null) plano.setAtivo(request.getAtivo());
        if (request.getRecursos() != null) plano.setRecursos(request.getRecursos());

        if (request.getValorAnualCentavos() != null &&
            !request.getValorAnualCentavos().equals(plano.getValorAnualCentavos())) {
            plano.setValorAnualCentavos(request.getValorAnualCentavos());
            valorAlterado = true;
        }

        plano = planoRepository.save(plano);

        // Se valor mudou, precisa criar novo plano no PagBank (PagBank não permite editar valor)
        if (valorAlterado && plano.getPagbankPlanId() != null) {
            log.warn("Valor do plano {} alterado. Criando novo plano no PagBank...", plano.getNome());
            try {
                sincronizarComPagBank(plano.getId());
                log.info("Novo plano criado no PagBank para {}", plano.getNome());
            } catch (Exception e) {
                log.error("Erro ao criar novo plano no PagBank: {}", e.getMessage());
            }
        }

        return plano;
    }

    @Override
    @Transactional
    public void sincronizarComPagBank(UUID planoId) {
        var plano = planoRepository.findById(planoId)
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado"));

        log.info("Sincronizando plano {} com PagBank...", plano.getNome());

        var pagBankPlanDto = PagBankPlanDto.builder()
                .name(plano.getNome() + " - Anual")
                .description(plano.getDescricao())
                .amount(PagBankPlanDto.PagBankAmountDto.builder()
                        .value(plano.getValorAnualCentavos().intValue())
                        .currency("BRL")
                        .build())
                .interval(PagBankPlanDto.PagBankIntervalDto.builder()
                        .unit("YEAR")
                        .length(1)
                        .build())
                // billingCycles omitido = recorrência infinita
                .trial(PagBankPlanDto.PagBankTrialDto.builder()
                        .enabled(false)
                        .build())
                .paymentMethod(List.of("CREDIT_CARD"))
                .build();

        var response = pagBankService.criarPlano(pagBankPlanDto);
        plano.setPagbankPlanId(response.getId());
        planoRepository.save(plano);

        log.info("Plano {} sincronizado. PagBank ID: {}", plano.getNome(), response.getId());
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
