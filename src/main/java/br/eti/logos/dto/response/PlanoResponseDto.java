package br.eti.logos.dto.response;

import br.eti.logos.enums.PlanoTierEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoResponseDto implements Serializable {

    private UUID id;
    private String nome;
    private String descricao;
    private PlanoTierEnum tier;
    private Integer limiteUsuarios;
    private BigDecimal valorAnualCentavos;
    private Boolean ativo;
    private String pagbankPlanId;
    private List<String> recursos;
}
