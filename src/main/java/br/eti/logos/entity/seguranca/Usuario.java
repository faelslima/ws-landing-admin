package br.eti.logos.entity.seguranca;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "tb_user", schema = "seguranca")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @UuidGenerator
    @GeneratedValue
    @Column(length = 36)
    private String id;

    private String nome;

    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    private String password;

    @Column(name = "igreja_id", length = 36)
    private String igrejaId;

    private Boolean ativo;

    @Column(name = "is_system_admin")
    private Boolean isSystemAdmin;
}
