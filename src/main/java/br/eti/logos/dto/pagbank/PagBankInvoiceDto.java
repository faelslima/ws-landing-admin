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
public class PagBankInvoiceDto {

    private String id;

    @JsonProperty("subscription_id")
    private String subscriptionId;

    private String status;

    private PagBankInvoiceAmountDto amount;

    @JsonProperty("due_date")
    private String dueDate;

    @JsonProperty("paid_at")
    private String paidAt;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("decline_reason")
    private String declineReason;

    @JsonProperty("payment_response")
    private PagBankPaymentResponseDto paymentResponse;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankInvoiceAmountDto {
        private Integer value;
        private String currency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankPaymentResponseDto {
        private String code;
        private String message;
        private String reference;
    }
}
