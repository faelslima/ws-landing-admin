package br.eti.logos.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^[A-Z0-9]{2}\\.[A-Z0-9]{3}\\.[A-Z0-9]{3}/\\d{4}-\\d{2}$", message = "CNPJ inválido")
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
