package br.eti.logos.service.onboarding.impl;

import br.eti.logos.core.util.MoneyUtil;
import br.eti.logos.core.validation.CnpjValidator;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final LeadRepository leadRepository;
    private final PlanoRepository planoRepository;
    private final LicencaRepository licencaRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final IgrejaRepository igrejaRepository;
    private final PagBankService pagBankService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.landing.exchange}")
    private String exchange;

    @Value("${rabbitmq.landing.saga.provisioning.routing.key}")
    private String sagaProvisioningRoutingKey;

    @Override
    @Transactional
    @CacheEvict(value = {"leads", "dashboard"}, allEntries = true)
    public Lead registrarLead(LeadRequestDto request) {
        log.info("Registrando lead: {}", request.getEmail());

        var lead = Lead.builder()
                .nomeIgreja(request.getNomeIgreja())
                .nomeResponsavel(request.getNomeResponsavel())
                .email(request.getEmail())
                .telefone(request.getTelefone())
                .cnpj(CnpjValidator.strip(request.getCnpj()))
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
    @Caching(evict = {
        @CacheEvict(value = "leads", allEntries = true),
        @CacheEvict(value = "licencas", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public String iniciarCheckout(CheckoutRequestDto request) {
        log.info("Iniciando checkout para: {}", request.getEmail());

        var plano = planoRepository.findById(request.getPlanoId())
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado"));

        if (plano.getPagbankPlanId() == null) {
            throw new IllegalStateException("Plano não sincronizado com PagBank");
        }

        // Upsert de lead: cria NOVO se não existe, promove para QUALIFICADO se já existe
        var cnpjStripped = CnpjValidator.strip(request.getCnpj());
        var lead = leadRepository.findTopByEmailOrderByCriadoEmDesc(request.getEmail())
                .orElseGet(() -> Lead.builder()
                        .nomeIgreja(request.getNomeIgreja())
                        .nomeResponsavel(request.getNomeResponsavel())
                        .email(request.getEmail())
                        .telefone(request.getTelefone())
                        .cnpj(cnpjStripped)
                        .status(LeadStatusEnum.NOVO)
                        .build());

        if (lead.getStatus() == LeadStatusEnum.NOVO || lead.getStatus() == LeadStatusEnum.CONTATADO) {
            lead.setStatus(LeadStatusEnum.QUALIFICADO);
        }
        leadRepository.saveAndFlush(lead);

        var telefoneDigits = request.getTelefone().replaceAll("\\D", "");

        // Se já existe customer com este CPF no PagBank, reusa o ID existente para evitar o erro
        // "customer cannot be created, as there is already a customer registered with the informed tax_ID"
        var customer = resolverCustomer(request, telefoneDigits);

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

        var pagbankResponse = pagBankService.criarAssinatura(subscriptionDto);

        // igrejaId gerado aqui, enviado na saga para ws-security criar a Igreja com o mesmo UUID
        var igrejaId = UUID.randomUUID().toString();

        var igreja = Igreja.builder()
                .id(igrejaId)
                .razaoSocial(request.getNomeIgreja())
                .nomeFantasia(request.getNomeIgreja())
                .cnpj(cnpjStripped)
                .email(request.getEmail())
                .telefone(request.getTelefone())
                .nomeResponsavel(request.getNomeResponsavel())
                .ativo(true)
                .build();
        igrejaRepository.save(igreja);

        var licenca = Licenca.builder()
                .igrejaId(igrejaId)
                .plano(plano)
                .status(LicencaStatusEnum.TRIAL)
                .dataInicio(OffsetDateTime.now())
                .dataExpiracao(OffsetDateTime.now().plusYears(1))
                .build();
        licencaRepository.saveAndFlush(licenca);

        var assinatura = Assinatura.builder()
                .licenca(licenca)
                .pagbankSubscriptionId(pagbankResponse.getId())
                .pagbankPlanId(plano.getPagbankPlanId())
                .status(AssinaturaStatusEnum.PENDING)
                .valorAnual(plano.getValorAnual())
                .emailCliente(request.getEmail())
                .build();
        assinaturaRepository.saveAndFlush(assinatura);

        boolean pagamentoAprovado = registrarPagamentoInicial(assinatura, pagbankResponse.getId(), plano.getValorAnual());

        if (pagamentoAprovado) {
            ativarAssinaturaEDispararOnboarding(assinatura, licenca, lead);
        }

        log.info("Checkout criado. subscription={} igrejaId={}", pagbankResponse.getId(), igrejaId);
        return pagbankResponse.getId();
    }

    private void ativarAssinaturaEDispararOnboarding(Assinatura assinatura, Licenca licenca, Lead lead) {
        assinatura.setStatus(AssinaturaStatusEnum.ACTIVE);
        assinaturaRepository.save(assinatura);

        licenca.setStatus(LicencaStatusEnum.ATIVA);
        licencaRepository.save(licenca);

        if (lead.getStatus() != LeadStatusEnum.CONVERTIDO) {
            lead.setStatus(LeadStatusEnum.CONVERTIDO);
            lead.setIgrejaIdConvertida(licenca.getIgrejaId());
            lead.setDataConversao(OffsetDateTime.now());
            leadRepository.save(lead);
            publicarProvisionamento(licenca, lead);
            log.info("Onboarding disparado no checkout: lead={} igrejaId={}", lead.getEmail(), licenca.getIgrejaId());
        }
    }

    private boolean registrarPagamentoInicial(Assinatura assinatura, String subscriptionId, java.math.BigDecimal valorPlano) {
        try {
            var invoicesDto = pagBankService.listarFaturasAdmin(subscriptionId);
            var invoices = invoicesDto != null ? invoicesDto.getInvoices() : null;
            if (invoices == null || invoices.isEmpty()) {
                log.warn("Nenhuma invoice encontrada logo após criação da subscription: {}", subscriptionId);
                return false;
            }

            var invoice = invoices.get(0);

            var existente = pagamentoRepository.findByPagbankInvoiceId(invoice.getId());
            if (existente.isPresent()) {
                return "PAID".equalsIgnoreCase(invoice.getStatus());
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

            return statusPagamento == PagamentoStatusEnum.PAID;
        } catch (Exception e) {
            log.error("Falha ao registrar pagamento inicial da subscription {}: {}", subscriptionId, e.getMessage());
            return false;
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

    private String extrairMotivoRecusa(br.eti.logos.dto.pagbank.InvoicesListDto.InvoiceDetail invoice) {
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
    @Caching(evict = {
        @CacheEvict(value = "leads", allEntries = true),
        @CacheEvict(value = "licencas", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public void processarPagamentoConfirmado(String pagbankSubscriptionId) {
        log.info("Processando confirmação de pagamento para subscription: {}", pagbankSubscriptionId);

        var assinatura = assinaturaRepository.findByPagbankSubscriptionId(pagbankSubscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada: " + pagbankSubscriptionId));

        assinatura.setStatus(AssinaturaStatusEnum.ACTIVE);
        assinaturaRepository.save(assinatura);

        var licenca = assinatura.getLicenca();
        licenca.setStatus(LicencaStatusEnum.ATIVA);
        licencaRepository.save(licenca);

        leadRepository.findTopByEmailOrderByCriadoEmDesc(assinatura.getEmailCliente())
                .filter(l -> l.getStatus() != LeadStatusEnum.CONVERTIDO)
                .ifPresent(lead -> {
                    lead.setStatus(LeadStatusEnum.CONVERTIDO);
                    lead.setIgrejaIdConvertida(licenca.getIgrejaId());
                    lead.setDataConversao(OffsetDateTime.now());
                    leadRepository.save(lead);
                    publicarProvisionamento(licenca, lead);
                    log.info("Onboarding concluído para lead: {} igrejaId: {}", lead.getEmail(), licenca.getIgrejaId());
                });
    }

    private PagBankCustomerDto resolverCustomer(CheckoutRequestDto request, String telefoneDigits) {
        try {
            var cpfLimpo = request.getCpfResponsavel().replaceAll("\\D", "");
            var resultado = pagBankService.listarClientes(cpfLimpo, 0, 1);
            if (resultado != null
                    && resultado.getCustomers() != null
                    && !resultado.getCustomers().isEmpty()) {
                var existente = resultado.getCustomers().get(0);
                log.info("Customer já existe no PagBank para CPF {}: id={}", cpfLimpo, existente.getId());
                // Passa apenas o ID — PagBank reutiliza o customer existente
                return PagBankCustomerDto.builder()
                        .id(existente.getId())
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
            }
        } catch (Exception e) {
            log.warn("Não foi possível verificar customer existente no PagBank (CPF {}): {}. Prosseguindo com criação.",
                    request.getCpfResponsavel(), e.getMessage());
        }

        // Customer não existe — manda objeto completo para criação
        return PagBankCustomerDto.builder()
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
    }

    private void publicarProvisionamento(Licenca licenca, Lead lead) {
        try {
            var plano = licenca.getPlano();
            var event = OnboardingProvisioningEvent.builder()
                    .igrejaId(licenca.getIgrejaId())
                    .razaoSocial(lead.getNomeIgreja())
                    .nomeFantasia(lead.getNomeIgreja())
                    .cnpj(lead.getCnpj())
                    .email(lead.getEmail())
                    .telefone(lead.getTelefone())
                    .nomeResponsavel(lead.getNomeResponsavel())
                    .planoNome(plano != null ? plano.getNome() : null)
                    .licencaId(licenca.getId() != null ? licenca.getId().toString() : null)
                    .limiteUsuarios(plano != null ? plano.getLimiteUsuarios() : null)
                    .lang("pt")
                    .build();
            rabbitTemplate.convertAndSend(exchange, sagaProvisioningRoutingKey, event);
            log.info("Evento de provisionamento publicado: igrejaId={} email={}", licenca.getIgrejaId(), lead.getEmail());
        } catch (Exception e) {
            log.error("Falha ao publicar provisionamento para igrejaId={}: {}", licenca.getIgrejaId(), e.getMessage());
        }
    }
}
