package br.eti.logos.repository;

import br.eti.logos.entity.landing.Pagamento;
import br.eti.logos.enums.PagamentoStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    Optional<Pagamento> findByPagbankInvoiceId(String pagbankInvoiceId);

    Page<Pagamento> findAllByAssinaturaLicencaIgrejaId(UUID igrejaId, Pageable pageable);

    Page<Pagamento> findAllByStatus(PagamentoStatusEnum status, Pageable pageable);

    List<Pagamento> findAllByAssinaturaIdAndStatus(UUID assinaturaId, PagamentoStatusEnum status);

    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p WHERE p.status = 'PAID' AND p.dataPagamento >= :desde")
    BigDecimal somaReceitaDesde(@Param("desde") OffsetDateTime desde);

    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p WHERE p.status = 'PAID'")
    BigDecimal somaReceitaTotal();

    @Query("""
        SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p
        WHERE p.status = 'PAID'
        AND p.assinatura.licenca.plano.id = :planoId
    """)
    BigDecimal somaReceitaPorPlano(@Param("planoId") UUID planoId);
}
