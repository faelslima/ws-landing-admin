package br.eti.logos.dto.response;

import br.eti.logos.enums.LicencaStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicencaResponseDto implements Serializable {

    private UUID id;
    private String igrejaId;
    private String nomeIgreja;
    private String nomeResponsavel;
    private String planoNome;
    private LicencaStatusEnum status;
    private Integer limiteUsuarios;
    private Integer usuariosAtivos;
    private Integer percentualUso;
    private String dataInicio;          // ISO 8601
    private String dataExpiracao;       // ISO 8601
    private String dataProximaCobranca; // ISO 8601
}
