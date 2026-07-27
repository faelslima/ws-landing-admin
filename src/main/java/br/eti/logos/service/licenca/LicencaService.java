package br.eti.logos.service.licenca;

import br.eti.logos.dto.request.LicencaManualRequestDto;
import br.eti.logos.dto.response.LicencaResponseDto;
import br.eti.logos.enums.LicencaStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LicencaService {

    Page<LicencaResponseDto> listar(LicencaStatusEnum status, Pageable pageable);

    LicencaResponseDto buscarPorIgreja(String igrejaId);

    /**
     * Libera ou renova manualmente a licença de uma igreja (sem pagamento).
     * Cria a licença se a igreja ainda não tiver uma, ou atualiza plano/vigência
     * da existente. Reativa a igreja e dispara a saga (provisionamento na primeira
     * liberação; reativação nas renovações).
     */
    LicencaResponseDto criarOuAtualizarLicencaManual(String igrejaId, LicencaManualRequestDto request);

    void suspender(UUID licencaId, String motivo);

    void reativar(UUID licencaId);

    void cancelar(UUID licencaId, String motivo);

    void inativarIgreja(String igrejaId, String motivo);

    void verificarLicencasExpiradas();
}
