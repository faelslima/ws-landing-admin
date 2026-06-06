package br.eti.logos.entity.landing;

import br.eti.logos.enums.LicencaStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_licenca", schema = "landing",
        indexes = {
                @Index(name = "idx_licenca_igreja", columnList = "igreja_id"),
                @Index(name = "idx_licenca_status", columnList = "status")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Licenca  implements Serializable {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "igreja_id", nullable = false, length = 36)
    private String igrejaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false, foreignKey = @ForeignKey(name = "fk_licenca_plano_id"))
    private Plano plano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LicencaStatusEnum status;

    @Column(nullable = false)
    private OffsetDateTime dataInicio;

    @Column(nullable = false)
    private OffsetDateTime dataExpiracao;

    private OffsetDateTime dataSuspensao;

    private OffsetDateTime dataCancelamento;

    private String motivoCancelamento;

    private OffsetDateTime criadoEm;

    private OffsetDateTime atualizadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = OffsetDateTime.now();
        this.atualizadoEm = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = OffsetDateTime.now();
    }
}
