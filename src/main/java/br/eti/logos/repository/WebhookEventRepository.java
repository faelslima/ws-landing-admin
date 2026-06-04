package br.eti.logos.repository;

import br.eti.logos.entity.landing.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    Optional<WebhookEvent> findByPayloadHash(String payloadHash);

    Optional<WebhookEvent> findByPagbankEventId(String pagbankEventId);

    boolean existsByPayloadHash(String payloadHash);
}
