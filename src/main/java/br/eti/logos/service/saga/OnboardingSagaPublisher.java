package br.eti.logos.service.saga;

import br.eti.logos.dto.saga.LicenseReactivationEvent;
import br.eti.logos.dto.saga.LicenseSuspensionEvent;
import br.eti.logos.dto.saga.OnboardingProvisioningEvent;
import br.eti.logos.entity.igreja.Igreja;
import br.eti.logos.entity.landing.Plano;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publicador central dos eventos da saga de onboarding/licença para o ws-security.
 * Concentra o disparo de provisionamento, suspensão e reativação para que os
 * fluxos manuais (admin) e automáticos (webhook/pagamento) compartilhem o mesmo
 * contrato de mensageria.
 *
 * <p>Falhas de publicação são logadas e não propagadas — o estado local (banco
 * landing) já foi persistido pelo chamador dentro da transação; a reconciliação
 * com o ws-security é responsabilidade da saga/DLQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnboardingSagaPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.landing.exchange}")
    private String exchange;

    @Value("${rabbitmq.landing.saga.provisioning.routing.key}")
    private String provisioningRoutingKey;

    @Value("${rabbitmq.landing.saga.license.suspension.routing.key}")
    private String suspensionRoutingKey;

    @Value("${rabbitmq.landing.saga.license.reactivation.routing.key}")
    private String reactivationRoutingKey;

    /**
     * Dispara a criação de igreja + usuário admin + descendências no ws-security.
     * Mesmo contrato do fluxo de checkout, porém sem depender de pagamento.
     */
    public void publicarProvisionamento(Igreja igreja, Plano plano, UUID licencaId, String lang) {
        try {
            var event = OnboardingProvisioningEvent.builder()
                    .igrejaId(igreja.getId())
                    .razaoSocial(igreja.getRazaoSocial())
                    .nomeFantasia(igreja.getNomeFantasia())
                    .cnpj(igreja.getCnpj())
                    .email(igreja.getEmail())
                    .telefone(igreja.getTelefone())
                    .nomeResponsavel(igreja.getNomeResponsavel())
                    .planoNome(plano != null ? plano.getNome() : null)
                    .licencaId(licencaId != null ? licencaId.toString() : null)
                    .limiteUsuarios(plano != null ? plano.getLimiteUsuarios() : null)
                    .lang(lang != null ? lang : "pt")
                    .build();
            rabbitTemplate.convertAndSend(exchange, provisioningRoutingKey, event);
            log.info("Provisionamento manual publicado: igrejaId={} email={}", igreja.getId(), igreja.getEmail());
        } catch (Exception e) {
            log.error("Falha ao publicar provisionamento manual para igrejaId={}: {}", igreja.getId(), e.getMessage());
        }
    }

    /**
     * Sinaliza ao ws-security para inativar a igreja e os usuários vinculados.
     */
    public void publicarSuspensao(String igrejaId, String assinaturaId, String motivo) {
        try {
            var event = LicenseSuspensionEvent.builder()
                    .igrejaId(igrejaId)
                    .assinaturaId(assinaturaId)
                    .motivo(motivo)
                    .build();
            rabbitTemplate.convertAndSend(exchange, suspensionRoutingKey, event);
            log.info("Suspensão publicada: igrejaId={} motivo={}", igrejaId, motivo);
        } catch (Exception e) {
            log.error("Falha ao publicar suspensão para igrejaId={}: {}", igrejaId, e.getMessage());
        }
    }

    /**
     * Sinaliza ao ws-security para reativar a igreja e os usuários vinculados.
     */
    public void publicarReativacao(String igrejaId, UUID licencaId, Plano plano) {
        try {
            var event = LicenseReactivationEvent.builder()
                    .igrejaId(igrejaId)
                    .licencaId(licencaId != null ? licencaId.toString() : null)
                    .limiteUsuarios(plano != null ? plano.getLimiteUsuarios() : null)
                    .planoNome(plano != null ? plano.getNome() : null)
                    .build();
            rabbitTemplate.convertAndSend(exchange, reactivationRoutingKey, event);
            log.info("Reativação publicada: igrejaId={}", igrejaId);
        } catch (Exception e) {
            log.error("Falha ao publicar reativação para igrejaId={}: {}", igrejaId, e.getMessage());
        }
    }
}
