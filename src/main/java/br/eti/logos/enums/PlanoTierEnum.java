package br.eti.logos.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Getter
@RequiredArgsConstructor
public enum PlanoTierEnum implements Serializable {

    STARTER("Starter", 50),
    GROWTH("Growth", 200),
    PROFESSIONAL("Professional", 500),
    ENTERPRISE("Enterprise", Integer.MAX_VALUE);

    private final String descricao;
    private final int limiteUsuarios;
}
