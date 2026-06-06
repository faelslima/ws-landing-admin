package br.eti.logos.entity.landing;

import br.eti.logos.enums.PlanoTierEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_plano", schema = "landing")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plano implements Serializable {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanoTierEnum tier;

    @Column(nullable = false)
    private Integer limiteUsuarios;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorAnualCentavos;

    @Column(nullable = false)
    private Boolean ativo;

    private String pagbankPlanId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_plano_recursos", schema = "landing", joinColumns = @JoinColumn(name = "plano_id"), foreignKey = @ForeignKey(name="fk_recurso_plano_id"))
    @Column(name = "recurso")
    private List<String> recursos;

    private OffsetDateTime criadoEm;

    private OffsetDateTime atualizadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = OffsetDateTime.now();
        this.atualizadoEm = OffsetDateTime.now();
        if (this.ativo == null) this.ativo = true;
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = OffsetDateTime.now();
    }
}
