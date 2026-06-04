package br.eti.logos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {

    private Long totalIgrejasAtivas;
    private Long totalIgrejasSuspensas;
    private Long totalLeads;
    private Long leadsConvertidos;

    private BigDecimal receitaMensalRecorrente;
    private BigDecimal receitaAnual;

    private Long totalUsuariosAtivos;
    private Long assinaturasAtivas;
    private Long assinaturasVencidas;

    private List<AlertaUsuarioDto> alertasLimiteUsuarios;
    private List<ReceitaPorPlanoDto> receitaPorPlano;
    private List<ConversaoMensalDto> conversaoMensal;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertaUsuarioDto {
        private String nomeIgreja;
        private Integer usuariosAtivos;
        private Integer limiteUsuarios;
        private Integer percentualUso;
        private String plano;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceitaPorPlanoDto {
        private String plano;
        private Long quantidade;
        private BigDecimal receita;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversaoMensalDto {
        private String mes;
        private Long leads;
        private Long convertidos;
        private Double taxaConversao;
    }
}
