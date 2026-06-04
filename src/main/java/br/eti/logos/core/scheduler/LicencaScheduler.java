package br.eti.logos.core.scheduler;

import br.eti.logos.service.licenca.LicencaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LicencaScheduler {

    private final LicencaService licencaService;

    @Scheduled(cron = "0 0 2 * * *")
    public void verificarLicencasExpiradas() {
        log.info("Iniciando verificação de licenças expiradas");
        licencaService.verificarLicencasExpiradas();
    }
}
