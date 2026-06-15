package br.eti.logos.repository;

import br.eti.logos.entity.landing.Lead;
import br.eti.logos.enums.LeadStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Page<Lead> findAllByStatus(LeadStatusEnum status, Pageable pageable);

    Page<Lead> findAllByOrderByCriadoEmDesc(Pageable pageable);

    Optional<Lead> findTopByEmailOrderByCriadoEmDesc(String email);

    Long countByStatus(LeadStatusEnum status);

    Optional<Lead> findTopByIgrejaIdConvertidaOrderByCriadoEmDesc(String igrejaId);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.criadoEm >= :desde")
    Long countDesde(@Param("desde") OffsetDateTime desde);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.status = 'CONVERTIDO' AND l.dataConversao >= :desde")
    Long countConvertidosDesde(@Param("desde") OffsetDateTime desde);
}
