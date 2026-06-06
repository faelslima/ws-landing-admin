package br.eti.logos.entity.landing;

import br.eti.logos.enums.WebhookEventTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_webhook_event", schema = "landing",
        indexes = {
                @Index(name = "idx_webhook_pagbank_id", columnList = "pagbank_event_id"),
                @Index(name = "idx_webhook_type", columnList = "tipo"),
                @Index(name = "idx_webhook_payload_hash", columnList = "payload_hash", unique = true)
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_event_pagbank_event_id", columnNames = "pagbank_event_id"),
                @UniqueConstraint(name = "uk_event_payload_hash", columnNames = "payload_hash"),
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent implements Serializable {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "pagbank_event_id", unique = true)
    private String pagbankEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "tipo")
    private WebhookEventTypeEnum tipo;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "payload_hash", nullable = false, unique = true, length = 64)
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
