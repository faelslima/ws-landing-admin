package br.eti.logos.entity.igreja;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "tb_igreja", schema = "comum")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Igreja {

    @Id
    @Column(length = 36)
    @UuidGenerator
    @GeneratedValue
    private String id;

    @Column(nullable = false)
    private String razaoSocial;

    private String nomeFantasia;

    private String sigla;

    private String cnpj;

    @Column(name = "igreja_mae_id")
    private String igrejaMaeId;

    private Boolean ativo;

    private String email;

    private String telefone;

    @PrePersist
    public void prePersist() {
        if (this.ativo == null) this.ativo = true;
    }
}
