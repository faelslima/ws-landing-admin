package br.eti.logos.service.assinatura;

import br.eti.logos.dto.response.AssinaturaResponseDto;
import br.eti.logos.enums.AssinaturaStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AssinaturaService {

    Page<AssinaturaResponseDto> listar(AssinaturaStatusEnum status, Pageable pageable);

    AssinaturaResponseDto buscarPorId(UUID id);

    AssinaturaResponseDto buscarPorPagbankId(String pagbankSubscriptionId);
}
