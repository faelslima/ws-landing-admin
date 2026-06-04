package br.eti.logos.dto.response;

import br.eti.logos.enums.LicencaStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicencaResponseDto {

    private UUID id;
    private String igrejaId;
    private String nomeIgreja;
    private String planoNome;
    private LicencaStatusEnum status;
    private Integer limiteUsuarios;
    private Integer usuariosAtivos;
    private Integer percentualUso;
    private OffsetDateTime dataInicio;
    private OffsetDateTime dataExpiracao;
    private OffsetDateTime dataProximaCobranca;
}
