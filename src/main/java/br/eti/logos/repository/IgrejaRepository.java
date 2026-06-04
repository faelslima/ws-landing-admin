package br.eti.logos.repository;

import br.eti.logos.entity.igreja.Igreja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IgrejaRepository extends JpaRepository<Igreja, String> {

    Optional<Igreja> findByCnpj(String cnpj);

    Long countByAtivoTrue();
}
