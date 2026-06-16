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
public class LicenseSuspensionEvent implements Serializable {

    private String igrejaId;
    private String assinaturaId;
    private String motivo;
    private String retryUrl;
}
