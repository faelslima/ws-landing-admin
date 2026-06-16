package br.eti.logos.service.onboarding;

import br.eti.logos.dto.request.CheckoutRetryRequestDto;
import br.eti.logos.dto.response.CheckoutRetryInfoDto;
import br.eti.logos.entity.landing.Assinatura;

public interface CheckoutRetryService {

    CheckoutRetryInfoDto buscarInfoRetry(String token);

    void executarRetry(String token, CheckoutRetryRequestDto request);

    String gerarTokenParaAssinatura(Assinatura assinatura);
}
