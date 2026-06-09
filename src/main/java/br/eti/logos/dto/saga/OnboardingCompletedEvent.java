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
public class OnboardingCompletedEvent implements Serializable {

    private String igrejaId;
    private String email;
    private String nomeIgreja;
    private String nomeResponsavel;
    private String lang;
}
