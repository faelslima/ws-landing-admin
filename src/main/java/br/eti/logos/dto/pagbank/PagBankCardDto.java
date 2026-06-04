package br.eti.logos.dto.pagbank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagBankCardDto {

    private String encrypted;

    @JsonProperty("security_code")
    private Integer securityCode;

    private PagBankCardHolderDto holder;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankCardHolderDto {
        private String name;

        @JsonProperty("tax_id")
        private String taxId;
    }
}
