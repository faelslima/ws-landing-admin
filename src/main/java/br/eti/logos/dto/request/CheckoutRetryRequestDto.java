package br.eti.logos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CheckoutRetryRequestDto {

    @NotNull
    private UUID planoId;

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
