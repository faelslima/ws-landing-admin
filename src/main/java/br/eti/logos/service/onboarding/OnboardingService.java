package br.eti.logos.service.onboarding;

import br.eti.logos.dto.request.CheckoutRequestDto;
import br.eti.logos.dto.request.LeadRequestDto;
import br.eti.logos.entity.landing.Lead;

public interface OnboardingService {

    Lead registrarLead(LeadRequestDto request);

    String iniciarCheckout(CheckoutRequestDto request);

    void processarPagamentoConfirmado(String pagbankSubscriptionId);
}
