package br.eti.logos.dto.pagbank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
@JsonPropertyOrder({"reference_id", "plan", "customer", "payment_method", "amount", "pro_rata"})
public class PagBankSubscriptionDto {

    @JsonProperty("reference_id")
    private String referenceId;

    private PagBankPlanRef plan;
    private PagBankCustomerDto customer;
    private PagBankSubscriptionAmountDto amount;

    @JsonProperty("payment_method")
    private List<PagBankPaymentMethodDto> paymentMethod;

    @JsonProperty("pro_rata")
    private Boolean proRata;

    // Response fields
    private String id;
    private String status;

    @JsonProperty("next_invoice_at")
    private String nextInvoiceAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankPlanRef {
        private String id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankSubscriptionAmountDto {
        private Integer value;
        private String currency;
    }
}
