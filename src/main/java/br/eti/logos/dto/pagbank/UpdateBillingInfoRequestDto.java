package br.eti.logos.dto.pagbank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de um item do array enviado em PUT /customers/{id}/billing_info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateBillingInfoRequestDto {

    private String type;   // CREDIT_CARD
    private Card card;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Card {
        private String number;

        @JsonProperty("security_code")
        private Integer securityCode;

        @JsonProperty("exp_month")
        private String expMonth;

        @JsonProperty("exp_year")
        private String expYear;

        private Holder holder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Holder {
        private String name;

        @JsonProperty("tax_id")
        private String taxId;

        @JsonProperty("birth_date")
        private String birthDate;

        private Phone phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Phone {
        private String country;
        private String area;
        private String number;
    }
}
