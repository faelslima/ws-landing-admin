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
 * DTO de resposta para listagem de clientes (customers) do PagBank
 * Mapeado para o JSON retornado por GET /customers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomersListDto implements Serializable {

    @JsonProperty("result_set")
    private ResultSet resultSet;

    private List<Customer> customers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultSet implements Serializable {
        private Integer total;
        private Integer offset;
        private Integer limit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Customer implements Serializable {
        private String id;
        private String name;
        private String email;

        @JsonProperty("tax_id")
        private String taxId;

        private List<Phone> phones;

        @JsonProperty("billing_info")
        private List<BillingInfo> billingInfo;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("updated_at")
        private String updatedAt;

        private List<Link> links;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Phone implements Serializable {
        private Integer id;
        private String country;
        private String area;
        private String number;
        private String type;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BillingInfo implements Serializable {
        private String type;
        private CardInfo card;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
    public static class Link implements Serializable {
        private String rel;
        private String href;
        private String media;
        private String type;
    }
}
