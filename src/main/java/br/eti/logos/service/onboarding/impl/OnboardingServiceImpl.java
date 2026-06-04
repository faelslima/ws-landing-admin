package br.eti.logos.service.onboarding.impl;

import br.eti.logos.dto.pagbank.*;
import br.eti.logos.dto.request.CheckoutRequestDto;
import br.eti.logos.dto.request.LeadRequestDto;
import br.eti.logos.entity.igreja.Igreja;
import br.eti.logos.entity.landing.*;
import br.eti.logos.entity.seguranca.Usuario;
import br.eti.logos.enums.AssinaturaStatusEnum;
import br.eti.logos.enums.LeadStatusEnum;
import br.eti.logos.enums.LicencaStatusEnum;
import br.eti.logos.repository.*;
import br.eti.logos.service.email.EmailService;
import br.eti.logos.service.pagbank.PagBankService;
import br.eti.logos.service.onboarding.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final LeadRepository leadRepository;
    private final PlanoRepository planoRepository;
    private final IgrejaRepository igrejaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LicencaRepository licencaRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final PagBankService pagBankService;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;

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
                        .value(plano.getValorAnualCentavos().intValue())
                        .currency("BRL")
                        .build())
                .proRata(false)
                .build();

        log.debug("Payload PagBank Subscription: {}", subscriptionDto);

        var pagbankResponse = pagBankService.criarAssinatura(subscriptionDto);

        // Salvar lead como qualificado
        leadRepository.findAllByOrderByCriadoEmDesc(null).stream()
                .filter(l -> l.getEmail().equals(request.getEmail()))
                .findFirst()
                .ifPresent(lead -> {
                    lead.setStatus(LeadStatusEnum.QUALIFICADO);
                    leadRepository.save(lead);
                });

        // Criar igreja
        var igreja = Igreja.builder()
                .razaoSocial(request.getNomeIgreja())
                .nomeFantasia(request.getNomeIgreja())
                .cnpj(request.getCnpj())
                .email(request.getEmail())
                .telefone(request.getTelefone())
                .ativo(false) // ativada após confirmação de pagamento
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
                .valorAnual(plano.getValorAnualCentavos())
                .build();
        assinaturaRepository.save(assinatura);

        log.info("Checkout criado. PagBank subscription: {}", pagbankResponse.getId());
        return pagbankResponse.getId();
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

        // Ativar igreja
        var igreja = igrejaRepository.findById(licenca.getIgrejaId()).orElse(null);
        if (igreja != null && !igreja.getAtivo()) {
            igreja.setAtivo(true);
            igrejaRepository.save(igreja);

            // Criar usuário admin da igreja
            criarUsuarioAdmin(igreja);

            // Enviar email de boas-vindas
            emailService.enviarBoasVindas(
                    igreja.getEmail(),
                    igreja.getRazaoSocial(),
                    "Administrador",
                    "pt"
            );
        }

        // Atualizar lead como convertido
        leadRepository.findAllByOrderByCriadoEmDesc(null).stream()
                .filter(l -> igreja != null && l.getEmail().equals(igreja.getEmail())
                        && l.getStatus() != LeadStatusEnum.CONVERTIDO)
                .findFirst()
                .ifPresent(lead -> {
                    lead.setStatus(LeadStatusEnum.CONVERTIDO);
                    lead.setIgrejaIdConvertida(igreja.getId());
                    lead.setDataConversao(OffsetDateTime.now());
                    leadRepository.save(lead);
                });

        log.info("Onboarding concluído para igreja: {}", igreja != null ? igreja.getRazaoSocial() : "N/A");
    }

    private void criarUsuarioAdmin(Igreja igreja) {
        if (usuarioRepository.existsByEmail(igreja.getEmail())) {
            log.warn("Usuário já existe para email: {}", igreja.getEmail());
            return;
        }

        var usuario = new Usuario();
        usuario.setId(UUID.randomUUID().toString());
        usuario.setNome("Administrador");
        usuario.setUsername(igreja.getEmail());
        usuario.setEmail(igreja.getEmail());
        usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setIgrejaId(igreja.getId());
        usuario.setAtivo(true);
        usuario.setIsSystemAdmin(false);
        usuarioRepository.save(usuario);

        log.info("Usuário admin criado para igreja: {}", igreja.getRazaoSocial());
    }
}
