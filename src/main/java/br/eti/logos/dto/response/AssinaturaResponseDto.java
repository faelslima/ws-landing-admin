package br.eti.logos.dto.response;

import br.eti.logos.enums.AssinaturaStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssinaturaResponseDto implements Serializable {

    private UUID id;
    private String pagbankSubscriptionId;
    private String nomeIgreja;
    private String planoNome;
    private BigDecimal valorAnual;
    private AssinaturaStatusEnum status;
    private String dataCriacao;       // ISO 8601
    private String dataProximaFatura; // ISO 8601
    private String dataCancelamento;  // ISO 8601
}
