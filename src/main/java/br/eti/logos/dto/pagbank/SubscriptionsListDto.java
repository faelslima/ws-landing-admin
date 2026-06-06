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
 * DTO de resposta para listagem de assinaturas do PagBank
 * Mapeado para o JSON retornado por GET /subscriptions
 *
 * @author Rafael Lima
 * @since 2026-06-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionsListDto implements Serializable {

    @JsonProperty("result_set")
    private ResultSet resultSet;

    private List<PagBankSubscriptionResponseDto> subscriptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultSet implements Serializable {
        private Integer total;
        private Integer offset;
        private Integer limit;
    }
}
