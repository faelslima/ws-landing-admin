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
 * DTO de resposta para listagem de faturas (invoices) do PagBank
 * Mapeado para o JSON retornado por GET /subscriptions/{id}/invoices
 *
 * @author Rafael Lima
 * @since 2026-06-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoicesListDto implements Serializable {

    @JsonProperty("result_set")
    private ResultSet resultSet;

    private List<InvoiceDetail> invoices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultSet implements Serializable {
        private Integer total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceDetail implements Serializable {
        private String id;                  // INVO_XXXXXXXX-...
        private Amount amount;
        private String status;              // PAID, OVERDUE, UNPAID, WAITING
        private PlanSummary plan;
        private List<InvoiceItem> items;
        private SubscriptionRef subscription;
        private Integer occurrence;         // Número do ciclo
        private CustomerSummary customer;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("updated_at")
        private String updatedAt;

        @JsonProperty("due_date")
        private String dueDate;

        @JsonProperty("paid_at")
        private String paidAt;

        private List<Link> links;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Amount implements Serializable {
        private Integer value;
        private String currency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanSummary implements Serializable {
        private String id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceItem implements Serializable {
        private Amount amount;
        private String type;                // SUBSCRIPTION_AMOUNT, COUPON_DISCOUNT, etc.
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionRef implements Serializable {
        private String id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerSummary implements Serializable {
        private String id;
        private String name;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Link implements Serializable {
        private String rel;
        private String href;
        private String media;
        private String type;
    }
}
