package br.eti.logos.dto.pagbank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagBankCustomerDto {

    private String id;
    private String name;
    private String email;

    @JsonProperty("tax_id")
    private String taxId;

    private List<PagBankPhoneDto> phones;

    @JsonProperty("billing_info")
    private List<BillingInfo> billingInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingInfo {
        private String type;
        private PagBankCardDto card;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankPhoneDto {
        private String country;
        private String area;
        private String number;
        private String type;
    }
}
