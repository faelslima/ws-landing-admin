package br.eti.logos.repository;

import br.eti.logos.entity.igreja.Igreja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface IgrejaRepository extends JpaRepository<Igreja, String>, JpaSpecificationExecutor<Igreja> {

    Optional<Igreja> findByCnpj(String cnpj);

    Long countByAtivoTrue();
}
