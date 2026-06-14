package br.eti.logos.entity.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_log_erros", schema = "logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogErro {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "controller", length = 50)
    private String endpoint;

    private LocalDateTime dataHora;

    @Column(name = "method", length = 10)
    private String httpMethod;

    @Column(columnDefinition = "text")
    private String stackTrace;
}
