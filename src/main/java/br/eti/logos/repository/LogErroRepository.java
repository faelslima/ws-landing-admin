package br.eti.logos.repository;

import br.eti.logos.entity.audit.LogErro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LogErroRepository extends JpaRepository<LogErro, UUID> {
}
