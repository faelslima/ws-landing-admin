package br.eti.logos.core.scheduler;

import br.eti.logos.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {

    private final EmailService emailService;

    @Value("${alert.email.admin}")
    private String emailAdmin;

    @Scheduled(cron = "${alert.user-limit.cron:0 0 8 * * *}")
    public void verificarLimiteUsuarios() {
        log.debug("verificarLimiteUsuarios: contagem de usuários migrada para ws-security — scheduler desabilitado");
    }
}
