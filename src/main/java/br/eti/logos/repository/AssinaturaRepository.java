package br.eti.logos.repository;

import br.eti.logos.entity.landing.Assinatura;
import br.eti.logos.enums.AssinaturaStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssinaturaRepository extends JpaRepository<Assinatura, UUID> {

    Optional<Assinatura> findByPagbankSubscriptionId(String pagbankSubscriptionId);

    Optional<Assinatura> findByLicencaId(UUID licencaId);

    Page<Assinatura> findAllByStatus(AssinaturaStatusEnum status, Pageable pageable);

    Long countByStatus(AssinaturaStatusEnum status);
}
