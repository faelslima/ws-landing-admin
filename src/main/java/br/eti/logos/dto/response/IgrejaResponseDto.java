package br.eti.logos.dto.response;

import br.eti.logos.enums.LicencaStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Igreja com o resumo da sua licença atual (se houver), para as telas de
 * gestão manual do admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IgrejaResponseDto implements Serializable {

    private String id;
    private String razaoSocial;
    private String nomeFantasia;
    private String sigla;
    private String cnpj;
    private String email;
    private String telefone;
    private String nomeResponsavel;
    private Boolean ativo;

    // Resumo da licença vinculada (null quando a igreja não possui licença)
    private String licencaId;
    private String planoNome;
    private LicencaStatusEnum licencaStatus;
    private Integer limiteUsuarios;
    private String dataInicio;     // ISO 8601
    private String dataExpiracao;  // ISO 8601 — null = prazo indeterminado
    private boolean prazoIndeterminado;
}
