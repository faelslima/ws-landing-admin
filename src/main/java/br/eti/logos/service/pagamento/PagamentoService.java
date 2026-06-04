package br.eti.logos.service.pagamento;

import br.eti.logos.dto.request.RefundRequestDto;
import br.eti.logos.dto.response.PagamentoResponseDto;
import br.eti.logos.enums.PagamentoStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PagamentoService {

    Page<PagamentoResponseDto> listar(PagamentoStatusEnum status, Pageable pageable);

    Page<PagamentoResponseDto> listarPorIgreja(UUID igrejaId, Pageable pageable);

    PagamentoResponseDto buscarPorId(UUID id);

    void estornar(RefundRequestDto request);

    void cancelar(UUID pagamentoId, String motivo);

    void retentarCobranca(UUID pagamentoId);
}
