package br.eti.logos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Requisição de criação manual de igreja pelo admin (sem checkout/pagamento).
 * A licença é opcional: se {@code licenca} vier preenchida, é criada junto e a
 * saga de provisionamento é disparada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IgrejaManualRequestDto implements Serializable {

    @NotBlank
    private String razaoSocial;

    private String nomeFantasia;

    private String sigla;

    private String cnpj;

    @Email
    private String email;

    private String telefone;

    private String nomeResponsavel;

    /**
     * Licença opcional a ser criada junto com a igreja. Quando {@code null},
     * apenas a igreja é criada (sem disparar a saga de provisionamento).
     */
    @Valid
    private LicencaManualRequestDto licenca;
}
