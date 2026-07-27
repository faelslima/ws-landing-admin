package br.eti.logos.service.igreja;

import br.eti.logos.dto.request.IgrejaManualRequestDto;
import br.eti.logos.dto.response.IgrejaResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Gestão manual de igrejas pelo admin (fora do fluxo de checkout/pagamento).
 */
public interface AdminIgrejaService {

    Page<IgrejaResponseDto> listar(String termo, Boolean ativo, Pageable pageable);

    IgrejaResponseDto buscar(String igrejaId);

    /**
     * Cria uma igreja manualmente. Se a requisição trouxer dados de licença,
     * cria a licença junto e dispara a saga de provisionamento.
     */
    IgrejaResponseDto criar(IgrejaManualRequestDto request);

    /** Inativa a igreja e revoga a licença, propagando ao ws-security. */
    void inativar(String igrejaId, String motivo);

    /** Reativa a igreja e a licença, propagando ao ws-security. */
    void reativar(String igrejaId);
}
