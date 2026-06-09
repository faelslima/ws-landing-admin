package br.eti.logos.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class CriarUsuarioRequestDto {

    private String fullname;
    private String username;
    private String email;
    private String password;
    private Set<String> roles;
    private Boolean ativo;
}
