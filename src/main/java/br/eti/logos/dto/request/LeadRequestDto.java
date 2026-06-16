package br.eti.logos.dto.request;

import br.eti.logos.core.validation.ValidCnpj;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadRequestDto {

    @NotBlank
    private String nomeIgreja;

    @NotBlank
    private String nomeResponsavel;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String telefone;

    @ValidCnpj
    private String cnpj;

    private String cidade;

    private String estado;

    private Integer quantidadeMembros;

    private String observacao;
}
