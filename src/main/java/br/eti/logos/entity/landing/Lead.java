package br.eti.logos.entity.landing;

import br.eti.logos.enums.LeadStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_lead", schema = "landing",
        indexes = {
                @Index(name = "idx_lead_email", columnList = "email"),
                @Index(name = "idx_lead_status", columnList = "status")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lead {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String nomeIgreja;

    @Column(nullable = false)
    private String nomeResponsavel;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String telefone;

    private String cnpj;

    private String cidade;

    private String estado;

    private Integer quantidadeMembros;

    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatusEnum status;

    @Column(length = 36)
    private String igrejaIdConvertida;

    private OffsetDateTime dataConversao;

    private OffsetDateTime criadoEm;

    private OffsetDateTime atualizadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = OffsetDateTime.now();
        this.atualizadoEm = OffsetDateTime.now();
        if (this.status == null) this.status = LeadStatusEnum.NOVO;
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = OffsetDateTime.now();
    }
}
