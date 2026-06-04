package br.eti.logos.entity.landing;

import br.eti.logos.enums.FormaPagamentoEnum;
import br.eti.logos.enums.PagamentoStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_pagamento", schema = "landing",
        indexes = {
                @Index(name = "idx_pagamento_assinatura", columnList = "assinatura_id"),
                @Index(name = "idx_pagamento_status", columnList = "status"),
                @Index(name = "idx_pagamento_pagbank_invoice", columnList = "pagbankInvoiceId")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pagamento {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id", nullable = false)
    private Assinatura assinatura;

    @Column(unique = true)
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

    private OffsetDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = OffsetDateTime.now();
        if (this.valorEstornado == null) this.valorEstornado = BigDecimal.ZERO;
    }
}
