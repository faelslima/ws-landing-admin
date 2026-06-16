package br.eti.logos.service.onboarding.impl;

import br.eti.logos.core.util.MoneyUtil;
import br.eti.logos.dto.pagbank.*;
import br.eti.logos.dto.saga.LicenseReactivationEvent;
import br.eti.logos.dto.saga.OnboardingProvisioningEvent;
import br.eti.logos.dto.request.CheckoutRetryRequestDto;
import br.eti.logos.dto.response.CheckoutRetryInfoDto;
import br.eti.logos.entity.landing.*;
import br.eti.logos.enums.*;
import br.eti.logos.repository.*;
import br.eti.logos.service.email.EmailService;
import br.eti.logos.service.onboarding.CheckoutRetryService;
import br.eti.logos.service.pagbank.PagBankService;
import br.eti.logos.service.plano.PlanoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutRetryServiceImpl implements CheckoutRetryService {

    private final CheckoutRetryTokenRepository checkoutRetryTokenRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final LicencaRepository licencaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final LeadRepository leadRepository;
    private final PlanoRepository planoRepository;
    private final IgrejaRepository igrejaRepository;
    private final PagBankService pagBankService;
    private final PlanoService planoService;
    private final RabbitTemplate rabbitTemplate;
    private final EmailService emailService;

    @Value("${rabbitmq.landing.exchange}")
    private String exchange;

    @Value("${rabbitmq.landing.saga.provisioning.routing.key}")
    private String sagaProvisioningRoutingKey;

    @Value("${rabbitmq.landing.saga.license.reactivation.routing.key}")
    private String sagaLicenseReactivationRoutingKey;

    private static final int TOKEN_EXPIRY_DAYS = 7;

    @Override
    public String gerarTokenParaAssinatura(Assinatura assinatura) {
        var tokenBytes = new byte[48];
        new SecureRandom().nextBytes(tokenBytes);
        var token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        var retryToken = CheckoutRetryToken.builder()
                .token(token)
                .assinatura(assinatura)
                .usado(false)
                .expiraEm(OffsetDateTime.now().plusDays(TOKEN_EXPIRY_DAYS))
                .build();
        checkoutRetryTokenRepository.save(retryToken);

        log.info("Token de retry gerado para assinatura={}", assinatura.getId());
        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutRetryInfoDto buscarInfoRetry(String token) {
        var retryToken = resolverTokenValido(token);
        var assinatura = retryToken.getAssinatura();
        var licenca = assinatura.getLicenca();

        var lead = leadRepository.findTopByEmailOrderByCriadoEmDesc(assinatura.getEmailCliente())
                .orElseThrow(() -> new IllegalStateException("Lead não encontrado para email: " + assinatura.getEmailCliente()));

        var igreja = igrejaRepository.findById(licenca.getIgrejaId()).orElse(null);

        var planosDisponiveis = planoService.listarPlanosAtivos();

        return new CheckoutRetryInfoDto(
                igreja != null ? igreja.getRazaoSocial() : lead.getNomeIgreja(),
                lead.getCnpj(),
                lead.getNomeResponsavel(),
                lead.getEmail(),
                lead.getTelefone(),
                null,
                licenca.getPlano().getId().toString(),
                licenca.getPlano().getNome(),
                planosDisponiveis
        );
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "leads", allEntries = true),
        @CacheEvict(value = "licencas", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public void executarRetry(String token, CheckoutRetryRequestDto request) {
        var retryToken = resolverTokenValido(token);
        var assinaturaAntiga = retryToken.getAssinatura();
        var licencaAntiga = assinaturaAntiga.getLicenca();

        var lead = leadRepository.findTopByEmailOrderByCriadoEmDesc(assinaturaAntiga.getEmailCliente())
                .orElseThrow(() -> new IllegalStateException("Lead não encontrado"));

        var plano = planoRepository.findById(request.getPlanoId())
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado"));
        if (plano.getPagbankPlanId() == null) {
            throw new IllegalStateException("Plano não sincronizado com PagBank");
        }

        // Cancela a assinatura PagBank antiga (best-effort)
        try {
            pagBankService.cancelarAssinatura(assinaturaAntiga.getPagbankSubscriptionId());
            log.info("Assinatura PagBank cancelada no retry: {}", assinaturaAntiga.getPagbankSubscriptionId());
        } catch (Exception e) {
            log.warn("Falha ao cancelar assinatura PagBank antiga {}: {}", assinaturaAntiga.getPagbankSubscriptionId(), e.getMessage());
        }

        // Marca assinatura e licença antigas como canceladas
        assinaturaAntiga.setStatus(AssinaturaStatusEnum.CANCELED);
        assinaturaAntiga.setDataCancelamento(OffsetDateTime.now());
        assinaturaAntiga.setMotivoCancelamento("Substituída por retry do cliente");
        assinaturaRepository.save(assinaturaAntiga);

        licencaAntiga.setStatus(LicencaStatusEnum.CANCELADA);
        licencaAntiga.setDataCancelamento(OffsetDateTime.now());
        licencaAntiga.setMotivoCancelamento("Substituída por retry do cliente");
        licencaRepository.save(licencaAntiga);

        var customer = resolverCustomerParaRetry(lead, request);

        var referenceId = "RETRY-" + java.util.UUID.randomUUID().toString().substring(0, 8);
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

        // Reutiliza o mesmo igrejaId e atualiza os dados da licença
        var novaLicenca = Licenca.builder()
                .igrejaId(licencaAntiga.getIgrejaId())
                .plano(plano)
                .status(LicencaStatusEnum.TRIAL)
                .dataInicio(OffsetDateTime.now())
                .dataExpiracao(OffsetDateTime.now().plusYears(1))
                .build();
        licencaRepository.saveAndFlush(novaLicenca);

        var novaAssinatura = Assinatura.builder()
                .licenca(novaLicenca)
                .pagbankSubscriptionId(pagbankResponse.getId())
                .pagbankPlanId(plano.getPagbankPlanId())
                .status(AssinaturaStatusEnum.PENDING)
                .valorAnual(plano.getValorAnual())
                .emailCliente(lead.getEmail())
                .build();
        assinaturaRepository.saveAndFlush(novaAssinatura);

        registrarPagamentoInicial(novaAssinatura, pagbankResponse.getId(), plano);

        // Marca o token como usado
        retryToken.setUsado(true);
        retryToken.setUsadoEm(OffsetDateTime.now());
        checkoutRetryTokenRepository.save(retryToken);

        log.info("Retry de checkout concluído: novaSubscription={} igrejaId={}", pagbankResponse.getId(), novaLicenca.getIgrejaId());
    }

    private CheckoutRetryToken resolverTokenValido(String token) {
        var retryToken = checkoutRetryTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de retry inválido ou não encontrado"));

        if (Boolean.TRUE.equals(retryToken.getUsado())) {
            throw new IllegalStateException("Este link de retry já foi utilizado");
        }
        if (OffsetDateTime.now().isAfter(retryToken.getExpiraEm())) {
            throw new IllegalStateException("Este link de retry expirou");
        }
        return retryToken;
    }

    private PagBankCustomerDto resolverCustomerParaRetry(Lead lead, CheckoutRetryRequestDto request) {
        try {
            // PagBank busca customer por CPF do titular do cartão (fornecido no retry)
            var cpfLimpo = request.getCardHolderTaxId().replaceAll("\\D", "");
            var resultado = pagBankService.listarClientes(cpfLimpo, 0, 1);
            if (resultado != null && resultado.getCustomers() != null && !resultado.getCustomers().isEmpty()) {
                var existente = resultado.getCustomers().get(0);
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
            log.warn("Não foi possível verificar customer existente no retry: {}. Prosseguindo com criação.", e.getMessage());
        }

        var telefoneDigits = lead.getTelefone().replaceAll("\\D", "");
        return PagBankCustomerDto.builder()
                .name(lead.getNomeResponsavel())
                .email(lead.getEmail())
                .taxId(request.getCardHolderTaxId())
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

    private void registrarPagamentoInicial(Assinatura assinatura, String subscriptionId, br.eti.logos.entity.landing.Plano plano) {
        try {
            var invoicesDto = pagBankService.listarFaturasAdmin(subscriptionId);
            var invoices = invoicesDto != null ? invoicesDto.getInvoices() : null;
            if (invoices == null || invoices.isEmpty()) {
                log.warn("Nenhuma invoice encontrada logo após retry da subscription: {}", subscriptionId);
                return;
            }

            var invoice = invoices.get(0);
            if (pagamentoRepository.findByPagbankInvoiceId(invoice.getId()).isPresent()) return;

            var statusInvoice = invoice.getStatus() != null ? invoice.getStatus().toUpperCase() : "";
            var statusPagamento = switch (statusInvoice) {
                case "PAID" -> PagamentoStatusEnum.PAID;
                case "OVERDUE", "UNPAID" -> PagamentoStatusEnum.DECLINED;
                default -> PagamentoStatusEnum.PENDING;
            };

            var valorCentavos = invoice.getAmount() != null && invoice.getAmount().getValue() != null
                    ? invoice.getAmount().getValue() : 0;
            var valor = valorCentavos > 0 ? MoneyUtil.centavosParaReais(valorCentavos) : plano.getValorAnual();

            var pagamento = Pagamento.builder()
                    .assinatura(assinatura)
                    .pagbankInvoiceId(invoice.getId())
                    .status(statusPagamento)
                    .valor(valor)
                    .formaPagamento(FormaPagamentoEnum.CREDIT_CARD)
                    .dataPagamento("PAID".equalsIgnoreCase(invoice.getStatus()) ? OffsetDateTime.now() : null)
                    .build();
            pagamentoRepository.save(pagamento);

            if (statusPagamento == PagamentoStatusEnum.PAID) {
                ativarAssinaturaEDispararOnboarding(assinatura, assinatura.getLicenca(),
                        leadRepository.findTopByEmailOrderByCriadoEmDesc(assinatura.getEmailCliente()).orElse(null));
            }
        } catch (Exception e) {
            log.error("Falha ao registrar pagamento inicial do retry {}: {}", subscriptionId, e.getMessage());
        }
    }

    private void ativarAssinaturaEDispararOnboarding(Assinatura assinatura, Licenca licenca, br.eti.logos.entity.landing.Lead lead) {
        assinatura.setStatus(AssinaturaStatusEnum.ACTIVE);
        assinaturaRepository.save(assinatura);

        licenca.setStatus(LicencaStatusEnum.ATIVA);
        licencaRepository.save(licenca);

        if (lead == null) return;

        if (lead.getStatus() == LeadStatusEnum.CONVERTIDO) {
            // Igreja já existe no ws-security — apenas reativa
            publicarReativacao(licenca);
            log.info("Reativação publicada via retry: igrejaId={}", licenca.getIgrejaId());
            try {
                emailService.enviarReativacao(
                        lead.getEmail(),
                        lead.getNomeResponsavel(),
                        licenca.getPlano() != null ? licenca.getPlano().getNome() : "",
                        "pt"
                );
            } catch (Exception e) {
                log.warn("Falha ao enviar email de reativação para {}: {}", lead.getEmail(), e.getMessage());
            }
        } else {
            lead.setStatus(LeadStatusEnum.CONVERTIDO);
            lead.setIgrejaIdConvertida(licenca.getIgrejaId());
            lead.setDataConversao(OffsetDateTime.now());
            leadRepository.save(lead);
            publicarProvisionamento(licenca, lead);
            log.info("Onboarding disparado via retry: lead={} igrejaId={}", lead.getEmail(), licenca.getIgrejaId());
        }
    }

    private void publicarProvisionamento(Licenca licenca, br.eti.logos.entity.landing.Lead lead) {
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
        } catch (Exception e) {
            log.error("Falha ao publicar provisionamento no retry: igrejaId={}", licenca.getIgrejaId());
        }
    }

    private void publicarReativacao(Licenca licenca) {
        try {
            var plano = licenca.getPlano();
            var event = LicenseReactivationEvent.builder()
                    .igrejaId(licenca.getIgrejaId())
                    .licencaId(licenca.getId() != null ? licenca.getId().toString() : null)
                    .limiteUsuarios(plano != null ? plano.getLimiteUsuarios() : null)
                    .planoNome(plano != null ? plano.getNome() : null)
                    .build();
            rabbitTemplate.convertAndSend(exchange, sagaLicenseReactivationRoutingKey, event);
        } catch (Exception e) {
            log.error("Falha ao publicar reativação: igrejaId={}", licenca.getIgrejaId());
        }
    }
}
