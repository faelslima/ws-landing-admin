package br.eti.logos.service.lead;

import br.eti.logos.dto.response.LeadResponseDto;
import br.eti.logos.enums.LeadStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LeadService {

    Page<LeadResponseDto> listar(LeadStatusEnum status, Pageable pageable);

    LeadResponseDto buscarPorId(UUID id);

    LeadResponseDto atualizarStatus(UUID id, LeadStatusEnum novoStatus, String observacao);
}
