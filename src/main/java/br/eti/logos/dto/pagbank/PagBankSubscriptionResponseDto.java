package br.eti.logos.dto.pagbank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * DTO de resposta completo da API de Assinaturas PagBank
 * Mapeado para o JSON retornado por GET /subscriptions/{id}
 *
 * @author Rafael Lima
 * @since 2026-06-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagBankSubscriptionResponseDto implements Serializable {

    private String id;                          // SUBS_XXXXXXXX-...

    @JsonProperty("reference_id")
    private String referenceId;                 // ONBOARD-xxxxxxxx

    private Amount amount;
    private String status;                      // ACTIVE, SUSPENDED, CANCELED, OVERDUE, PENDING, EXPIRED
    private PlanSummary plan;

    @JsonProperty("payment_method")
    private List<PaymentMethod> paymentMethod;

    @JsonProperty("next_invoice_at")
    private String nextInvoiceAt;               // YYYY-MM-DD ou ISO 8601

    @JsonProperty("billing_cycle")
    private BillingCycle billingCycle;

    @JsonProperty("pro_rata")
    private Boolean proRata;

    private CustomerSummary customer;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    private List<RetryInfo> retries;

    @JsonProperty("split_enabled")
    private Boolean splitEnabled;

    private List<Link> links;

    // ========================================================================
    // Inner Classes
    // ========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Amount implements Serializable {
        private Integer value;      // Em centavos
        private String currency;    // BRL
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanSummary implements Serializable {
        private String id;
        private String name;
        private Interval interval;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Interval implements Serializable {
        private Integer length;
        private String unit;        // DAY, MONTH, YEAR
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerSummary implements Serializable {
        private String id;          // CUST_XXXXXXXX-...
        private String name;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethod implements Serializable {
        private String type;        // CREDIT_CARD, DEBIT_CARD, BOLETO
        private CardInfo card;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardInfo implements Serializable {
        private String token;

        private String brand;

        @JsonProperty("first_digits")
        private String firstDigits;

        @JsonProperty("last_digits")
        private String lastDigits;

        @JsonProperty("exp_month")
        private String expMonth;

        @JsonProperty("exp_year")
        private String expYear;

        private CardHolder holder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardHolder implements Serializable {
        private String name;

        @JsonProperty("tax_id")
        private String taxId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingCycle implements Serializable {
        private Integer occurrence;  // Número do ciclo (1, 2, 3...)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryInfo implements Serializable {
        private String attempt;      // FIRST, SECOND, THIRD

        @JsonProperty("retried_at")
        private String retriedAt;    // YYYY-MM-DD

        private String status;       // SCHEDULED, AUTOMATICALLY_EXECUTED, FAILED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Link implements Serializable {
        private String rel;          // SELF, INVOICE, INVOICES.LAST, etc.
        private String href;
        private String media;
        private String type;         // GET, POST, PUT, DELETE
    }
}
