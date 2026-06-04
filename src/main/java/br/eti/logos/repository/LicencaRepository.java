package br.eti.logos.repository;

import br.eti.logos.entity.landing.Licenca;
import br.eti.logos.enums.LicencaStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LicencaRepository extends JpaRepository<Licenca, UUID> {

    Optional<Licenca> findByIgrejaId(String igrejaId);

    Optional<Licenca> findByIgrejaIdAndStatus(String igrejaId, LicencaStatusEnum status);

    List<Licenca> findAllByStatus(LicencaStatusEnum status);

    Page<Licenca> findAllByStatus(LicencaStatusEnum status, Pageable pageable);

    @Query("SELECT COUNT(l) FROM Licenca l WHERE l.status = :status")
    Long countByStatus(@Param("status") LicencaStatusEnum status);

    @Query("SELECT l FROM Licenca l WHERE l.status = 'ATIVA' AND l.dataExpiracao < CURRENT_TIMESTAMP")
    List<Licenca> findExpiradas();
}
