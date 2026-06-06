package br.eti.logos.service.plano;

import br.eti.logos.dto.request.PlanoCreateRequestDto;
import br.eti.logos.dto.response.PlanoResponseDto;
import br.eti.logos.entity.landing.Plano;

import java.util.List;
import java.util.UUID;

public interface PlanoService {

    List<PlanoResponseDto> listarTodos();

    List<PlanoResponseDto> listarPlanosAtivos();

    Plano criar(PlanoCreateRequestDto request);

    Plano atualizar(UUID id, PlanoCreateRequestDto request);

    void sincronizarComPagBank(UUID planoId);
}
