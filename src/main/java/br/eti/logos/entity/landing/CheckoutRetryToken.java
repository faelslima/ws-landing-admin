package br.eti.logos.entity.landing;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "tb_checkout_retry_token",
    schema = "landing",
    indexes = {
        @Index(name = "idx_retry_token_token", columnList = "token"),
        @Index(name = "idx_retry_token_assinatura", columnList = "assinatura_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRetryToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ass_check_retry_id"))
    private Assinatura assinatura;

    @Column(name = "usado", nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(name = "expira_em", nullable = false)
    private OffsetDateTime expiraEm;

    @Column(name = "usado_em")
    private OffsetDateTime usadoEm;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false, nullable = false)
    private OffsetDateTime criadoEm;
}
