package br.eti.logos.entity.igreja;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Entity
@Table(name = "tb_igreja", schema = "comum")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Igreja implements Persistable<String> {

    @Id
    @Column(length = 36)
    @UuidGenerator
    @GeneratedValue
    private String id;

    @Transient
    @JsonIgnore
    @Builder.Default
    private boolean isNew = true;

    @Override
    @JsonIgnore
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Column(nullable = false)
    private String razaoSocial;

    private String nomeFantasia;

    private String sigla;

    private String cnpj;

    @Column(name = "igreja_mae_id")
    private String igrejaMaeId;

    private Boolean ativo;

    private String nomeResponsavel;

    private String email;

    private String telefone;

    @PrePersist
    public void prePersist() {
        if (this.ativo == null) this.ativo = true;
    }
}
