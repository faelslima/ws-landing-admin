package br.eti.logos.dto.request;

import br.eti.logos.core.validation.ValidCnpj;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequestDto {

    @NotNull
    private UUID planoId;

    @NotBlank
    private String nomeIgreja;

    @NotBlank
    @ValidCnpj
    private String cnpj;

    @NotBlank
    private String nomeResponsavel;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String telefone;

    @NotBlank
    private String cpfResponsavel;

    @NotBlank
    private String encryptedCard;

    @NotBlank
    private String cardHolderName;

    @NotBlank
    private String cardHolderTaxId;

    @NotBlank
    private String cardSecurityCode;

    private String lang;
}
