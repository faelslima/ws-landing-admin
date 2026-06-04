package br.eti.logos.repository;

import br.eti.logos.entity.seguranca.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.igrejaId = :igrejaId AND u.ativo = true")
    Long countUsuariosAtivosByIgreja(@Param("igrejaId") String igrejaId);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.ativo = true")
    Long countAllAtivos();

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
