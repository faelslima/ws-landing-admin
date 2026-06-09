package br.eti.logos.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponseDto {

    private UserDto user;

    @JsonProperty("prefixToken")
    private String prefixToken;

    private String accessToken;

    private String expiresIn;

    private String refreshToken;

    private String refreshExpiresIn;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserDto {
        private String id;
        private String fullname;
        private String username;
        private String email;
        private Set<String> roles;
        private Boolean ativo;
    }
}
