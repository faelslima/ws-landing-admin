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
public class PagBankPlanDto {

    private String id;

    @JsonProperty("reference_id")
    private String referenceId;

    private String name;
    private String description;

    private PagBankAmountDto amount;
    private PagBankIntervalDto interval;

    @JsonProperty("billing_cycles")
    private Integer billingCycles;

    private PagBankTrialDto trial;

    @JsonProperty("payment_method")
    private List<String> paymentMethod;

    private String status;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankAmountDto {
        @JsonProperty("setup_fee")
        private Integer setupFee;

        private String currency;
        private Integer value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankIntervalDto {
        private String unit;
        private Integer length;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankTrialDto {
        private Boolean enabled;
        private Integer days;
    }
}
