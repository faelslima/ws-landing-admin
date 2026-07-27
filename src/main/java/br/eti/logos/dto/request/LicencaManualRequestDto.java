package br.eti.logos.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Requisição de liberação/manutenção manual de licença (sem pagamento).
 * Usada tanto na criação de igreja quanto no modal de manutenção por igreja.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicencaManualRequestDto implements Serializable {

    @NotNull
    private UUID planoId;

    /**
     * Quando {@code true}, a licença é por prazo indeterminado ({@code dataExpiracao}
     * é ignorada e persistida como {@code null}).
     */
    private boolean prazoIndeterminado;

    /**
     * Data de expiração (ISO 8601 ou data pura "yyyy-MM-dd").
     * Obrigatória quando {@code prazoIndeterminado} é {@code false}.
     */
    private String dataExpiracao;
}
