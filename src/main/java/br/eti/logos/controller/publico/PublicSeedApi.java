package br.eti.logos.controller.publico;

import br.eti.logos.entity.landing.Plano;
import br.eti.logos.enums.PlanoTierEnum;
import br.eti.logos.repository.PlanoRepository;
import br.eti.logos.service.plano.PlanoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/public/seed")
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class PublicSeedApi {

    private final PlanoRepository planoRepository;
    private final PlanoService planoService;

    @PostMapping("/planos")
    public ResponseEntity<String> seedPlanos() {
        log.info("Iniciando seed de planos...");

        // Limpar planos existentes
        planoRepository.deleteAll();

        // Criar planos de teste
        var starter = Plano.builder()
                .nome("Starter")
                .descricao("Ideal para igrejas pequenas iniciando sua jornada digital")
                .tier(PlanoTierEnum.STARTER)
                .limiteUsuarios(50)
                .valorAnualCentavos(new BigDecimal("119900")) // R$ 1.199,00/ano
                .ativo(true)
                .recursos(List.of(
                        "Até 50 usuários ativos",
                        "Gestão de membros",
                        "Controle financeiro básico",
                        "Relatórios mensais",
                        "Suporte por email"
                ))
                .build();

        var growth = Plano.builder()
                .nome("Growth")
                .descricao("Para igrejas em crescimento que precisam de mais recursos")
                .tier(PlanoTierEnum.GROWTH)
                .limiteUsuarios(200)
                .valorAnualCentavos(new BigDecimal("299900")) // R$ 2.999,00/ano
                .ativo(true)
                .recursos(List.of(
                        "Até 200 usuários ativos",
                        "Gestão de membros avançada",
                        "Controle financeiro completo",
                        "Gestão de eventos",
                        "Relatórios personalizados",
                        "Suporte prioritário"
                ))
                .build();

        var professional = Plano.builder()
                .nome("Professional")
                .descricao("Solução completa para igrejas de médio a grande porte")
                .tier(PlanoTierEnum.PROFESSIONAL)
                .limiteUsuarios(500)
                .valorAnualCentavos(new BigDecimal("599900")) // R$ 5.999,00/ano
                .ativo(true)
                .recursos(List.of(
                        "Até 500 usuários ativos",
                        "Todos os recursos do Growth",
                        "Gestão de células e ministérios",
                        "Dashboard executivo",
                        "Integração com apps externos",
                        "Suporte 24/7",
                        "Treinamento personalizado"
                ))
                .build();

        var enterprise = Plano.builder()
                .nome("Enterprise")
                .descricao("Para grandes igrejas com necessidades ilimitadas")
                .tier(PlanoTierEnum.ENTERPRISE)
                .limiteUsuarios(Integer.MAX_VALUE)
                .valorAnualCentavos(new BigDecimal("1199900")) // R$ 11.999,00/ano
                .ativo(true)
                .recursos(List.of(
                        "Usuários ilimitados",
                        "Todos os recursos do Professional",
                        "Customizações exclusivas",
                        "Consultoria estratégica",
                        "SLA garantido",
                        "Gestor de conta dedicado"
                ))
                .build();

        var planosLocais = List.of(starter, growth, professional, enterprise);
        planoRepository.saveAll(planosLocais);

        // Sincronizar com PagBank
        int sincronizados = 0;
        for (var plano : planosLocais) {
            try {
                planoService.sincronizarComPagBank(plano.getId());
                sincronizados++;
            } catch (Exception e) {
                log.error("Erro ao sincronizar plano {}: {}", plano.getNome(), e.getMessage());
            }
        }

        log.info("Seed de planos concluído! {} planos criados, {} sincronizados com PagBank.",
                planosLocais.size(), sincronizados);

        return ResponseEntity.ok(String.format(
                "Planos criados: %d locais, %d sincronizados com PagBank",
                planosLocais.size(), sincronizados));
    }

    @PostMapping("/sincronizar-todos")
    public ResponseEntity<String> sincronizarTodos() {
        log.info("Sincronizando todos os planos com PagBank...");

        var planos = planoRepository.findAllByAtivoTrue();
        int sincronizados = 0;

        for (var plano : planos) {
            try {
                planoService.sincronizarComPagBank(plano.getId());
                sincronizados++;
                log.info("Plano {} sincronizado", plano.getNome());
            } catch (Exception e) {
                log.error("Erro ao sincronizar {}: {}", plano.getNome(), e.getMessage());
            }
        }

        return ResponseEntity.ok(String.format(
                "%d de %d planos sincronizados com PagBank",
                sincronizados, planos.size()));
    }
}
