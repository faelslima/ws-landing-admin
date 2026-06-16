package br.eti.logos.dto.response;

import java.io.Serializable;
import java.util.List;

public record CheckoutRetryInfoDto(
    String nomeIgreja,
    String cnpj,
    String nomeResponsavel,
    String email,
    String telefone,
    String cpfResponsavel,
    String planoAtualId,
    String planoAtualNome,
    List<PlanoResponseDto> planosDisponiveis
) implements Serializable {}
