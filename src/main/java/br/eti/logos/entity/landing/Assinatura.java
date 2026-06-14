package br.eti.logos.entity.landing;

import br.eti.logos.enums.AssinaturaStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_assinatura", schema = "landing",
        indexes = {
                @Index(name = "idx_assinatura_pagbank_id", columnList = "pagbank_subscription_id"),
                @Index(name = "idx_assinatura_licenca", columnList = "licenca_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pagbank_subscription_id", columnNames = "pagbank_subscription_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assinatura  implements Serializable {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licenca_id", nullable = false, foreignKey = @ForeignKey(name = "fk_assinatura_licenca_id"))
    private Licenca licenca;

    @Column(name = "pagbank_subscription_id", unique = true)
    private String pagbankSubscriptionId;

    private String pagbankPlanId;

    private String emailCliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssinaturaStatusEnum status;

    @Column(precision = 12, scale = 2)
    private BigDecimal valorAnual;

    private OffsetDateTime dataProximaFatura;

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
