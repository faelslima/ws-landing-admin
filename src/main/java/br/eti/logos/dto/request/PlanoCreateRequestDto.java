package br.eti.logos.dto.request;

import br.eti.logos.enums.PlanoTierEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class PlanoCreateRequestDto {

    @NotBlank
    private String nome;

    private String descricao;

    @NotNull
    private PlanoTierEnum tier;

    @NotNull
    @Positive
    private Integer limiteUsuarios;

    @NotNull
    @Positive
    private BigDecimal valorAnualCentavos;

    private List<String> recursos;

    private Boolean ativo;
}
