package br.eti.logos.core.scheduler;

import br.eti.logos.enums.LicencaStatusEnum;
import br.eti.logos.repository.IgrejaRepository;
import br.eti.logos.repository.LicencaRepository;
import br.eti.logos.repository.UsuarioRepository;
import br.eti.logos.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {

    private final LicencaRepository licencaRepository;
    private final UsuarioRepository usuarioRepository;
    private final IgrejaRepository igrejaRepository;
    private final EmailService emailService;

    @Value("${alert.user-limit.threshold-percent:80}")
    private int thresholdPercent;

    @Value("${alert.email.admin}")
    private String emailAdmin;

    @Scheduled(cron = "${alert.user-limit.cron:0 0 8 * * *}")
    public void verificarLimiteUsuarios() {
        log.info("Iniciando verificação de limites de usuários");

        var licencasAtivas = licencaRepository.findAllByStatus(LicencaStatusEnum.ATIVA);
        var alertas = new ArrayList<String>();

        for (var licenca : licencasAtivas) {
            var usuariosAtivos = usuarioRepository.countUsuariosAtivosByIgreja(licenca.getIgrejaId());
            var limite = licenca.getPlano().getLimiteUsuarios();
            var percentual = limite > 0 ? (int) ((usuariosAtivos * 100) / limite) : 0;

            if (percentual >= thresholdPercent) {
                var igreja = igrejaRepository.findById(licenca.getIgrejaId()).orElse(null);
                var nomeIgreja = igreja != null ? igreja.getRazaoSocial() : "ID: " + licenca.getIgrejaId();

                var alerta = String.format(
                        "- %s: %d/%d usuarios (%d%%) - Plano: %s",
                        nomeIgreja, usuariosAtivos, limite, percentual, licenca.getPlano().getNome());
                alertas.add(alerta);

                if (percentual >= 100) {
                    log.warn("LIMITE EXCEDIDO: {} - {}/{} usuarios", nomeIgreja, usuariosAtivos, limite);
                } else {
                    log.info("ALERTA: {} atingindo limite - {}/{} ({}%)", nomeIgreja, usuariosAtivos, limite, percentual);
                }
            }
        }

        if (!alertas.isEmpty()) {
            enviarEmailAlerta(alertas);
        }
    }

    private void enviarEmailAlerta(ArrayList<String> alertas) {
        var html = "<h2>Alerta: Igrejas atingindo limite de usuarios</h2>"
                + "<p>As seguintes igrejas estao atingindo ou excederam o limite de usuarios do plano:</p>"
                + "<ul>" + alertas.stream().map(a -> "<li>" + a + "</li>").reduce("", String::concat) + "</ul>"
                + "<p>Acesse o painel administrativo para tomar acao.</p>";

        emailService.send(emailAdmin, "[i12 Admin] Alerta: Limite de usuarios", html);
        log.info("Email de alerta enviado com {} igrejas", alertas.size());
    }
}
