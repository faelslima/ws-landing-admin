package br.eti.logos.dto.response;

import br.eti.logos.enums.LeadStatusEnum;
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
public class LeadResponseDto {

    private UUID id;
    private String nomeIgreja;
    private String nomeResponsavel;
    private String email;
    private String telefone;
    private String cnpj;
    private String cidade;
    private String estado;
    private Integer quantidadeMembros;
    private String observacao;
    private LeadStatusEnum status;
    private String igrejaIdConvertida;
    private OffsetDateTime dataConversao;
    private OffsetDateTime criadoEm;
    private OffsetDateTime atualizadoEm;
}
