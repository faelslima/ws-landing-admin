package br.eti.logos.repository;

import br.eti.logos.entity.landing.Assinatura;
import br.eti.logos.enums.AssinaturaStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssinaturaRepository extends JpaRepository<Assinatura, UUID> {

    @EntityGraph(attributePaths = {"licenca", "licenca.plano"})
    Optional<Assinatura> findByPagbankSubscriptionId(String pagbankSubscriptionId);

    Optional<Assinatura> findByLicencaId(UUID licencaId);

    @EntityGraph(attributePaths = {"licenca", "licenca.plano"})
    Page<Assinatura> findAllByStatus(AssinaturaStatusEnum status, Pageable pageable);

    @EntityGraph(attributePaths = {"licenca", "licenca.plano"})
    Page<Assinatura> findAll(Pageable pageable);

    Long countByStatus(AssinaturaStatusEnum status);
}
