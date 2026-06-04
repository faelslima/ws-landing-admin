package br.eti.logos.service.dashboard.impl;

import br.eti.logos.dto.response.DashboardDto;
import br.eti.logos.entity.landing.Licenca;
import br.eti.logos.enums.AssinaturaStatusEnum;
import br.eti.logos.enums.LeadStatusEnum;
import br.eti.logos.enums.LicencaStatusEnum;
import br.eti.logos.repository.*;
import br.eti.logos.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final LicencaRepository licencaRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final LeadRepository leadRepository;
    private final PagamentoRepository pagamentoRepository;
    private final PlanoRepository planoRepository;
    private final UsuarioRepository usuarioRepository;
    private final IgrejaRepository igrejaRepository;

    @Value("${alert.user-limit.threshold-percent:80}")
    private int thresholdPercent;

    @Override
    public DashboardDto obterDashboard() {
        var totalIgrejasAtivas = licencaRepository.countByStatus(LicencaStatusEnum.ATIVA);
        var totalIgrejasSuspensas = licencaRepository.countByStatus(LicencaStatusEnum.SUSPENSA);
        var totalLeads = leadRepository.count();
        var leadsConvertidos = leadRepository.countByStatus(LeadStatusEnum.CONVERTIDO);
        var assinaturasAtivas = assinaturaRepository.countByStatus(AssinaturaStatusEnum.ACTIVE);
        var assinaturasVencidas = assinaturaRepository.countByStatus(AssinaturaStatusEnum.OVERDUE);
        var totalUsuariosAtivos = usuarioRepository.countAllAtivos();

        var receitaAnual = pagamentoRepository.somaReceitaTotal();
        var receitaMensal = receitaAnual.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        var alertas = calcularAlertas();
        var receitaPorPlano = calcularReceitaPorPlano();
        var conversaoMensal = calcularConversaoMensal();

        return DashboardDto.builder()
                .totalIgrejasAtivas(totalIgrejasAtivas)
                .totalIgrejasSuspensas(totalIgrejasSuspensas)
                .totalLeads(totalLeads)
                .leadsConvertidos(leadsConvertidos)
                .receitaMensalRecorrente(receitaMensal)
                .receitaAnual(receitaAnual)
                .totalUsuariosAtivos(totalUsuariosAtivos)
                .assinaturasAtivas(assinaturasAtivas)
                .assinaturasVencidas(assinaturasVencidas)
                .alertasLimiteUsuarios(alertas)
                .receitaPorPlano(receitaPorPlano)
                .conversaoMensal(conversaoMensal)
                .build();
    }

    private List<DashboardDto.AlertaUsuarioDto> calcularAlertas() {
        var alertas = new ArrayList<DashboardDto.AlertaUsuarioDto>();
        var licencasAtivas = licencaRepository.findAllByStatus(LicencaStatusEnum.ATIVA);

        for (var licenca : licencasAtivas) {
            var usuariosAtivos = usuarioRepository.countUsuariosAtivosByIgreja(licenca.getIgrejaId());
            var limite = licenca.getPlano().getLimiteUsuarios();
            var percentual = limite > 0 ? (int) ((usuariosAtivos * 100) / limite) : 0;

            if (percentual >= thresholdPercent) {
                var igreja = igrejaRepository.findById(licenca.getIgrejaId()).orElse(null);
                alertas.add(DashboardDto.AlertaUsuarioDto.builder()
                        .nomeIgreja(igreja != null ? igreja.getRazaoSocial() : "N/A")
                        .usuariosAtivos(usuariosAtivos.intValue())
                        .limiteUsuarios(limite)
                        .percentualUso(percentual)
                        .plano(licenca.getPlano().getNome())
                        .build());
            }
        }
        return alertas;
    }

    private List<DashboardDto.ReceitaPorPlanoDto> calcularReceitaPorPlano() {
        var planos = planoRepository.findAllByAtivoTrue();
        var resultado = new ArrayList<DashboardDto.ReceitaPorPlanoDto>();

        for (var plano : planos) {
            var receita = pagamentoRepository.somaReceitaPorPlano(plano.getId());
            var licencas = licencaRepository.findAllByStatus(LicencaStatusEnum.ATIVA).stream()
                    .filter(l -> l.getPlano().getId().equals(plano.getId()))
                    .count();

            resultado.add(DashboardDto.ReceitaPorPlanoDto.builder()
                    .plano(plano.getNome())
                    .quantidade(licencas)
                    .receita(receita)
                    .build());
        }
        return resultado;
    }

    private List<DashboardDto.ConversaoMensalDto> calcularConversaoMensal() {
        var resultado = new ArrayList<DashboardDto.ConversaoMensalDto>();
        var agora = OffsetDateTime.now();

        for (int i = 5; i >= 0; i--) {
            var inicio = agora.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            var fim = inicio.plusMonths(1);

            var leads = leadRepository.countDesde(inicio);
            var convertidos = leadRepository.countConvertidosDesde(inicio);
            var taxa = leads > 0 ? (convertidos * 100.0) / leads : 0.0;

            resultado.add(DashboardDto.ConversaoMensalDto.builder()
                    .mes(inicio.getMonth().name().substring(0, 3) + "/" + inicio.getYear())
                    .leads(leads)
                    .convertidos(convertidos)
                    .taxaConversao(taxa)
                    .build());
        }
        return resultado;
    }
}
