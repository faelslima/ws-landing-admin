package br.eti.logos.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = "^[A-Z0-9]{2}\\.[A-Z0-9]{3}\\.[A-Z0-9]{3}/\\d{4}-\\d{2}$", message = "CNPJ inválido")
    private String cnpj;

    private String cidade;

    private String estado;

    private Integer quantidadeMembros;

    private String observacao;
}
