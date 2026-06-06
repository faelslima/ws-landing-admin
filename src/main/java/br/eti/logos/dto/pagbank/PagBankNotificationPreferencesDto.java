package br.eti.logos.dto.pagbank;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class PagBankNotificationPreferencesDto {

    private List<String> urls;
    private EmailPreferences email;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailPreferences {
        private EmailConfig merchant;
        private EmailConfig customer;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailConfig {
        private Boolean enabled;
    }
}
