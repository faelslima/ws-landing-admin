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
public class LicenseReactivationEvent implements Serializable {

    private String igrejaId;
    private String licencaId;
    private Integer limiteUsuarios;
    private String planoNome;
}
