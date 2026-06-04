package br.eti.logos.entity.landing;

import br.eti.logos.enums.WebhookEventTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_webhook_event", schema = "landing",
        indexes = {
                @Index(name = "idx_webhook_pagbank_id", columnList = "pagbankEventId"),
                @Index(name = "idx_webhook_type", columnList = "tipo"),
                @Index(name = "idx_webhook_payload_hash", columnList = "payloadHash", unique = true)
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(unique = true)
    private String pagbankEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "tipo")
    private WebhookEventTypeEnum tipo;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, unique = true, length = 64)
    private String payloadHash;

    private Boolean processado;

    private String erroProcessamento;

    private OffsetDateTime recebidoEm;

    private OffsetDateTime processadoEm;

    @PrePersist
    public void prePersist() {
        this.recebidoEm = OffsetDateTime.now();
        if (this.processado == null) this.processado = false;
    }
}
