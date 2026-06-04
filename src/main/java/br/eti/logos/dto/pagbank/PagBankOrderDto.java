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
public class PagBankOrderDto {

    private String id;

    @JsonProperty("reference_id")
    private String referenceId;

    private PagBankCustomerDto customer;

    private List<PagBankOrderItemDto> items;

    @JsonProperty("notification_urls")
    private List<String> notificationUrls;

    private List<PagBankChargeDto> charges;

    @JsonProperty("qr_codes")
    private List<PagBankQrCodeDto> qrCodes;

    private String status;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankOrderItemDto {
        @JsonProperty("reference_id")
        private String referenceId;
        private String name;
        private Integer quantity;
        @JsonProperty("unit_amount")
        private Integer unitAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankChargeDto {
        private String id;
        @JsonProperty("reference_id")
        private String referenceId;
        private String description;
        private PagBankChargeAmountDto amount;
        @JsonProperty("payment_method")
        private PagBankChargePaymentMethodDto paymentMethod;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankChargeAmountDto {
        private Integer value;
        private String currency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankChargePaymentMethodDto {
        private String type;
        private Integer installments;
        private Boolean capture;
        @JsonProperty("soft_descriptor")
        private String softDescriptor;
        private PagBankCardDto card;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankQrCodeDto {
        private String id;
        private PagBankChargeAmountDto amount;
        @JsonProperty("expiration_date")
        private String expirationDate;
        private String text;
        private List<PagBankLinkDto> links;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagBankLinkDto {
        private String rel;
        private String href;
        private String media;
        private String type;
    }
}
