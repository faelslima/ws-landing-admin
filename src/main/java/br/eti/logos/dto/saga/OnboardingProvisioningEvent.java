package br.eti.logos.dto.saga;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingProvisioningEvent implements Serializable {

    private String igrejaId;
    private String razaoSocial;
    private String nomeFantasia;
    private String cnpj;
    private String email;
    private String telefone;
    private String nomeResponsavel;
    private String planoNome;
    private String lang;
}
