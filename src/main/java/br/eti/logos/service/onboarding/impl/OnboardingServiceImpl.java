package br.eti.logos.service.onboarding.impl;

import br.eti.logos.core.util.MoneyUtil;
import br.eti.logos.dto.pagbank.*;
import br.eti.logos.dto.request.CheckoutRequestDto;
import br.eti.logos.dto.request.LeadRequestDto;
import br.eti.logos.dto.saga.OnboardingProvisioningEvent;
import br.eti.logos.entity.igreja.Igreja;
import br.eti.logos.entity.landing.*;
import br.eti.logos.enums.*;
import br.eti.logos.repository.*;
import br.eti.logos.service.pagbank.PagBankService;
import br.eti.logos.service.onboarding.OnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LeadRepository leadRepository;
    private final PlanoRepository planoRepository;
    private final IgrejaRepository igrejaRepository;
    private final LicencaRepository licencaRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final PagBankService pagBankService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.landing.exchange}")
    private String exchange;

    @Value("${rabbitmq.landing.saga.provisioning.routing.key}")
    private String sagaProvisioningRoutingKey;

    @Override
    @Transactional
    public Lead registrarLead(LeadRequestDto request) {
        log.info("Registrando lead: {}", request.getEmail());

        var lead = Lead.builder()
                .nomeIgreja(request.getNomeIgreja())
                .nomeResponsavel(request.getNomeResponsavel())
                .email(request.getEmail())
                .telefone(request.getTelefone())
                .cnpj(request.getCnpj())
                .cidade(request.getCidade())
                .estado(request.getEstado())
                .quantidadeMembros(request.getQuantidadeMembros())
                .observacao(request.getObservacao())
                .status(LeadStatusEnum.NOVO)
                .build();

        return leadRepository.save(lead);
    }

    @Override
    @Transactional
    public String iniciarCheckout(CheckoutRequestDto request) {
        log.info("Iniciando checkout para: {}", request.getEmail());

        var plano = planoRepository.findById(request.getPlanoId())
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado"));

        if (plano.getPagbankPlanId() == null) {
            throw new IllegalStateException("Plano não sincronizado com PagBank");
        }

        // Upsert de lead: cria NOVO se não existe, promove para QUALIFICADO se já existe
        var lead = leadRepository.findTopByEmailOrderByCriadoEmDesc(request.getEmail())
                .orElseGet(() -> Lead.builder()
                        .nomeIgreja(request.getNomeIgreja())
                        .nomeResponsavel(request.getNomeResponsavel())
                        .email(request.getEmail())
                        .telefone(request.getTelefone())
                        .cnpj(request.getCnpj())
                        .status(LeadStatusEnum.NOVO)
                        .build());

        if (lead.getStatus() == LeadStatusEnum.NOVO || lead.getStatus() == LeadStatusEnum.CONTATADO) {
            lead.setStatus(LeadStatusEnum.QUALIFICADO);
        }
        leadRepository.save(lead);

        var telefoneDigits = request.getTelefone().replaceAll("\\D", "");

        var customer = PagBankCustomerDto.builder()
                .name(request.getNomeResponsavel())
                .email(request.getEmail())
                .taxId(request.getCpfResponsavel())
                .phones(List.of(PagBankCustomerDto.PagBankPhoneDto.builder()
                        .country("55")
                        .area(telefoneDigits.substring(0, 2))
                        .number(telefoneDigits.substring(2))
                        .build()))
                .billingInfo(List.of(PagBankCustomerDto.BillingInfo.builder()
                        .type("CREDIT_CARD")
                        .card(PagBankCardDto.builder()
                                .encrypted(request.getEncryptedCard())
                                .holder(PagBankCardDto.PagBankCardHolderDto.builder()
                                        .name(request.getCardHolderName())
                                        .taxId(request.getCardHolderTaxId())
                                        .build())
                                .build())
                        .build()))
                .build();

        var referenceId = "ONBOARD-" + UUID.randomUUID().toString().substring(0, 8);

        var subscriptionDto = PagBankSubscriptionDto.builder()
                .referenceId(referenceId)
                .plan(PagBankSubscriptionDto.PagBankPlanRef.builder()
                        .id(plano.getPagbankPlanId())
                        .build())
                .customer(customer)
                .paymentMethod(List.of(PagBankPaymentMethodDto.builder()
                        .type("CREDIT_CARD")
                        .card(PagBankCardDto.builder()
                                .securityCode(Integer.parseInt(request.getCardSecurityCode()))
                                .build())
                        .build()))
                .amount(PagBankSubscriptionDto.PagBankSubscriptionAmountDto.builder()
                        .value(MoneyUtil.reaisParaCentavos(plano.getValorAnual()))
                        .currency("BRL")
                        .build())
                .proRata(false)
                .build();

        log.debug("Payload PagBank Subscription: {}", subscriptionDto);

        var pagbankResponse = pagBankService.criarAssinatura(subscriptionDto);

        // Criar igreja
        var igreja = Igreja.builder()
                .razaoSocial(request.getNomeIgreja())
                .nomeFantasia(request.getNomeIgreja())
                .cnpj(request.getCnpj())
                .email(request.getEmail())
                .telefone(request.getTelefone())
                .ativo(false)
                .build();
        igrejaRepository.save(igreja);

        // Criar licença (pendente)
        var licenca = Licenca.builder()
                .igrejaId(igreja.getId())
                .plano(plano)
                .status(LicencaStatusEnum.TRIAL)
                .dataInicio(OffsetDateTime.now())
                .dataExpiracao(OffsetDateTime.now().plusYears(1))
                .build();
        licencaRepository.save(licenca);

        // Criar assinatura local
        var assinatura = Assinatura.builder()
                .licenca(licenca)
                .pagbankSubscriptionId(pagbankResponse.getId())
                .pagbankPlanId(plano.getPagbankPlanId())
                .status(AssinaturaStatusEnum.PENDING)
                .valorAnual(plano.getValorAnual())
                .build();
        assinaturaRepository.save(assinatura);

        // Registrar pagamento inicial com status vindo da invoice do PagBank
        registrarPagamentoInicial(assinatura, pagbankResponse.getId(), plano.getValorAnual());

        log.info("Checkout criado. PagBank subscription: {}", pagbankResponse.getId());
        return pagbankResponse.getId();
    }

    private void registrarPagamentoInicial(Assinatura assinatura, String subscriptionId, java.math.BigDecimal valorPlano) {
        try {
            var invoices = pagBankService.listarFaturas(subscriptionId);
            if (invoices == null || invoices.isEmpty()) {
                log.warn("Nenhuma invoice encontrada logo após criação da subscription: {}", subscriptionId);
                return;
            }

            var invoice = invoices.get(0);

            if (pagamentoRepository.findByPagbankInvoiceId(invoice.getId()).isPresent()) {
                return;
            }

            var statusPagamento = mapearStatusInvoice(invoice.getStatus());
            var valorCentavos = invoice.getAmount() != null && invoice.getAmount().getValue() != null
                    ? invoice.getAmount().getValue() : 0;
            var valor = valorCentavos > 0 ? MoneyUtil.centavosParaReais(valorCentavos) : valorPlano;

            var pagamento = Pagamento.builder()
                    .assinatura(assinatura)
                    .pagbankInvoiceId(invoice.getId())
                    .status(statusPagamento)
                    .valor(valor)
                    .formaPagamento(FormaPagamentoEnum.CREDIT_CARD)
                    .dataPagamento("PAID".equalsIgnoreCase(invoice.getStatus()) ? OffsetDateTime.now() : null)
                    .motivoRecusa(statusPagamento == PagamentoStatusEnum.DECLINED
                            ? extrairMotivoRecusa(invoice) : null)
                    .build();
            pagamentoRepository.save(pagamento);

            log.info("Pagamento inicial registrado: invoice={} status={} subscription={}",
                    invoice.getId(), statusPagamento, subscriptionId);
        } catch (Exception e) {
            log.error("Falha ao registrar pagamento inicial da subscription {}: {}", subscriptionId, e.getMessage());
        }
    }

    private PagamentoStatusEnum mapearStatusInvoice(String invoiceStatus) {
        if (invoiceStatus == null) return PagamentoStatusEnum.PENDING;
        return switch (invoiceStatus.toUpperCase()) {
            case "PAID" -> PagamentoStatusEnum.PAID;
            case "OVERDUE", "UNPAID" -> PagamentoStatusEnum.DECLINED;
            case "REFUNDED" -> PagamentoStatusEnum.REFUNDED;
            default -> PagamentoStatusEnum.PENDING;
        };
    }

    private String extrairMotivoRecusa(PagBankInvoiceDto invoice) {
        if (invoice.getDeclineReason() != null && !invoice.getDeclineReason().isBlank()) {
            return invoice.getDeclineReason();
        }
        if (invoice.getPaymentResponse() != null) {
            var code = invoice.getPaymentResponse().getCode();
            var message = invoice.getPaymentResponse().getMessage();
            if (code != null || message != null) {
                return String.format("Código: %s - %s",
                        code != null ? code : "N/A",
                        message != null ? message : "Recusado pela operadora");
            }
        }
        return "Pagamento recusado - verifique dados do cartão";
    }

    @Override
    @Transactional
    public void processarPagamentoConfirmado(String pagbankSubscriptionId) {
        log.info("Processando confirmação de pagamento para subscription: {}", pagbankSubscriptionId);

        var assinatura = assinaturaRepository.findByPagbankSubscriptionId(pagbankSubscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada: " + pagbankSubscriptionId));

        assinatura.setStatus(AssinaturaStatusEnum.ACTIVE);
        assinaturaRepository.save(assinatura);

        var licenca = assinatura.getLicenca();
        licenca.setStatus(LicencaStatusEnum.ATIVA);
        licencaRepository.save(licenca);

        var igreja = igrejaRepository.findById(licenca.getIgrejaId()).orElse(null);
        if (igreja != null && !igreja.getAtivo()) {
            igreja.setAtivo(true);
            igrejaRepository.save(igreja);

            publicarProvisionamento(igreja, licenca.getPlano().getNome());
        }

        if (igreja != null) {
            leadRepository.findTopByEmailOrderByCriadoEmDesc(igreja.getEmail())
                    .filter(l -> l.getStatus() != LeadStatusEnum.CONVERTIDO)
                    .ifPresent(lead -> {
                        lead.setStatus(LeadStatusEnum.CONVERTIDO);
                        lead.setIgrejaIdConvertida(igreja.getId());
                        lead.setDataConversao(OffsetDateTime.now());
                        leadRepository.save(lead);
                    });
        }

        log.info("Onboarding concluído para igreja: {}", igreja != null ? igreja.getRazaoSocial() : "N/A");
    }

    private void publicarProvisionamento(Igreja igreja, String planoNome) {
        try {
            var event = OnboardingProvisioningEvent.builder()
                    .igrejaId(igreja.getId())
                    .razaoSocial(igreja.getRazaoSocial())
                    .nomeFantasia(igreja.getNomeFantasia())
                    .cnpj(igreja.getCnpj())
                    .email(igreja.getEmail())
                    .telefone(igreja.getTelefone())
                    .nomeResponsavel("Administrador")
                    .planoNome(planoNome)
                    .lang("pt")
                    .build();
            var payload = OBJECT_MAPPER.writeValueAsString(event);
            rabbitTemplate.convertAndSend(exchange, sagaProvisioningRoutingKey, payload);
            log.info("Evento de provisionamento publicado para igreja: {} ({})", igreja.getRazaoSocial(), igreja.getId());
        } catch (Exception e) {
            log.error("Falha ao publicar evento de provisionamento para {}: {}", igreja.getRazaoSocial(), e.getMessage());
        }
    }
}
