package br.eti.logos.dto.response;

import br.eti.logos.enums.AssinaturaStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssinaturaResponseDto {

    private UUID id;
    private String pagbankSubscriptionId;
    private String nomeIgreja;
    private String planoNome;
    private BigDecimal valorAnual;
    private AssinaturaStatusEnum status;
    private OffsetDateTime dataCriacao;
    private OffsetDateTime dataProximaFatura;
    private OffsetDateTime dataCancelamento;
}
