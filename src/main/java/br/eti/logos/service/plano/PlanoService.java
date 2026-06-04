package br.eti.logos.service.plano;

import br.eti.logos.dto.request.PlanoCreateRequestDto;
import br.eti.logos.entity.landing.Plano;

import java.util.UUID;

public interface PlanoService {

    Plano criar(PlanoCreateRequestDto request);

    Plano atualizar(UUID id, PlanoCreateRequestDto request);

    void sincronizarComPagBank(UUID planoId);
}
