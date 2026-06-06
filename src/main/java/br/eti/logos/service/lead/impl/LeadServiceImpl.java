package br.eti.logos.service.lead.impl;

import br.eti.logos.core.util.DateTimeUtil;
import br.eti.logos.dto.response.LeadResponseDto;
import br.eti.logos.entity.landing.Lead;
import br.eti.logos.enums.LeadStatusEnum;
import br.eti.logos.repository.LeadRepository;
import br.eti.logos.service.lead.LeadService;
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
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;

    @Override
    @Cacheable(value = "leads", key = "#status != null ? #status.name() + '_' + #pageable.pageNumber : 'all_' + #pageable.pageNumber")
    public Page<LeadResponseDto> listar(LeadStatusEnum status, Pageable pageable) {
        log.debug("Buscando leads com status={}, page={} (cache miss)", status, pageable.getPageNumber());
        Page<Lead> page;
        if (status != null) {
            page = leadRepository.findAllByStatus(status, pageable);
        } else {
            page = leadRepository.findAllByOrderByCriadoEmDesc(pageable);
        }
        return page.map(this::toDto);
    }

    @Override
    @Cacheable(value = "leads", key = "'id_' + #id")
    public LeadResponseDto buscarPorId(UUID id) {
        log.debug("Buscando lead por id={} (cache miss)", id);
        var lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado"));
        return toDto(lead);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"leads", "dashboard"}, allEntries = true)
    public LeadResponseDto atualizarStatus(UUID id, LeadStatusEnum novoStatus, String observacao) {
        log.info("Atualizando status do lead id={} para {}", id, novoStatus);
        var lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado"));

        lead.setStatus(novoStatus);
        if (observacao != null) {
            lead.setObservacao(observacao);
        }

        lead = leadRepository.save(lead);
        log.info("Lead {} atualizado para status {}", id, novoStatus);

        return toDto(lead);
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
                .dataConversao(DateTimeUtil.toIsoString(lead.getDataConversao()))
                .criadoEm(DateTimeUtil.toIsoString(lead.getCriadoEm()))
                .atualizadoEm(DateTimeUtil.toIsoString(lead.getAtualizadoEm()))
                .build();
    }
}
