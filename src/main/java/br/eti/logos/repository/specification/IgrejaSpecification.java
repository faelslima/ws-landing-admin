package br.eti.logos.repository.specification;

import br.eti.logos.entity.igreja.Igreja;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;

/**
 * Filtros de busca de igrejas para a gestão manual do admin.
 * Usa Criteria (predicados tipados) — evita o problema de parâmetros nulos
 * tipados como {@code bytea} no PostgreSQL quando se usa JPQL com params opcionais.
 */
public final class IgrejaSpecification {

    private IgrejaSpecification() {
    }

    public static Specification<Igreja> filtro(String termo, Boolean ativo) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            if (ativo != null) {
                predicates.add(cb.equal(root.get("ativo"), ativo));
            }

            if (termo != null && !termo.isBlank()) {
                var like = "%" + termo.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("razaoSocial")), like),
                        cb.like(cb.lower(root.get("nomeFantasia")), like),
                        cb.like(cb.lower(root.get("cnpj")), like)
                ));
            }

            if (query != null) {
                query.orderBy(cb.asc(root.get("razaoSocial")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
