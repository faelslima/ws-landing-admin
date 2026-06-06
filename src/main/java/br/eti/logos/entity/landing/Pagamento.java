package br.eti.logos.entity.landing;

import br.eti.logos.enums.FormaPagamentoEnum;
import br.eti.logos.enums.PagamentoStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_pagamento", schema = "landing",
        indexes = {
                @Index(name = "idx_pagamento_assinatura", columnList = "assinatura_id"),
                @Index(name = "idx_pagamento_status", columnList = "status"),
                @Index(name = "idx_pagamento_pagbank_invoice", columnList = "pagbank_invoice_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pagbank_invoice_id", columnNames = "pagbank_invoice_id"),
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pagamento  implements Serializable {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pagamento_assinatura_id"))
    private Assinatura assinatura;

    @Column(name = "pagbank_invoice_id", unique = true)
    private String pagbankInvoiceId;

    private String pagbankChargeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PagamentoStatusEnum status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(precision = 12, scale = 2)
    private BigDecimal valorEstornado;

    @Enumerated(EnumType.STRING)
    private FormaPagamentoEnum formaPagamento;

    private OffsetDateTime dataVencimento;

    private OffsetDateTime dataPagamento;

    private OffsetDateTime dataEstorno;

    private String motivoEstorno;

    @Column(length = 1000)
    private String motivoRecusa;

    private OffsetDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = OffsetDateTime.now();
        if (this.valorEstornado == null) this.valorEstornado = BigDecimal.ZERO;
    }
}
