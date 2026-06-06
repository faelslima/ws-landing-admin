package br.eti.logos.dto.pagbank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de requisição para atualização de dados cadastrais de cliente (customer)
 * Mapeado para PUT /customers/{id}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateCustomerRequestDto {

    private String name;
    private String email;

    @JsonProperty("birth_date")
    private String birthDate;

    private List<Phone> phones;
    private Address address;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Phone {
        private String country;
        private String area;
        private String number;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Address {
        private String street;
        private String number;
        private String complement;

        @JsonProperty("region_code")
        private String regionCode;

        private String locality;
        private String city;

        @JsonProperty("postal_code")
        private String postalCode;

        private String country;
    }
}
