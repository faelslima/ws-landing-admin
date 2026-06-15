package br.eti.logos.service.plano;

import br.eti.logos.dto.request.PlanoCreateRequestDto;
import br.eti.logos.dto.response.PlanoResponseDto;

import java.util.List;
import java.util.UUID;

public interface PlanoService {

    List<PlanoResponseDto> listarTodos();

    List<PlanoResponseDto> listarPlanosAtivos();

    PlanoResponseDto criar(PlanoCreateRequestDto request);

    PlanoResponseDto atualizar(UUID id, PlanoCreateRequestDto request);

    void sincronizarComPagBank(UUID planoId);
}
