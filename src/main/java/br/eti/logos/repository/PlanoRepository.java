package br.eti.logos.repository;

import br.eti.logos.entity.landing.Plano;
import br.eti.logos.enums.PlanoTierEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanoRepository extends JpaRepository<Plano, UUID> {

    List<Plano> findAllByAtivoTrue();

    Optional<Plano> findByTier(PlanoTierEnum tier);

    Optional<Plano> findByPagbankPlanId(String pagbankPlanId);
}
