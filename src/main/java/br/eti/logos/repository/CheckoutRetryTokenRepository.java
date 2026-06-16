package br.eti.logos.repository;

import br.eti.logos.entity.landing.CheckoutRetryToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CheckoutRetryTokenRepository extends JpaRepository<CheckoutRetryToken, UUID> {

    Optional<CheckoutRetryToken> findByToken(String token);
}
