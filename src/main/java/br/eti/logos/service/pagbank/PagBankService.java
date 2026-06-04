package br.eti.logos.service.pagbank;

import br.eti.logos.dto.pagbank.PagBankInvoiceDto;
import br.eti.logos.dto.pagbank.PagBankPlanDto;
import br.eti.logos.dto.pagbank.PagBankSubscriptionDto;

import java.util.List;

public interface PagBankService {

    PagBankPlanDto criarPlano(PagBankPlanDto planDto);

    PagBankPlanDto consultarPlano(String planId);

    PagBankSubscriptionDto criarAssinatura(PagBankSubscriptionDto subscriptionDto);

    PagBankSubscriptionDto consultarAssinatura(String subscriptionId);

    void cancelarAssinatura(String subscriptionId);

    void suspenderAssinatura(String subscriptionId);

    void reativarAssinatura(String subscriptionId);

    List<PagBankInvoiceDto> listarFaturas(String subscriptionId);

    void retentarFatura(String invoiceId);

    void cancelarCobranca(String chargeId, Integer valorCentavos);
}
