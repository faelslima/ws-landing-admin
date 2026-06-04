package br.eti.logos.dto.response;

import br.eti.logos.enums.FormaPagamentoEnum;
import br.eti.logos.enums.PagamentoStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoResponseDto {

    private UUID id;
    private String pagbankInvoiceId;
    private String nomeIgreja;
    private String planoNome;
    private BigDecimal valor;
    private BigDecimal valorEstornado;
    private PagamentoStatusEnum status;
    private FormaPagamentoEnum formaPagamento;
    private OffsetDateTime dataPagamento;
    private OffsetDateTime dataVencimento;
    private OffsetDateTime dataCriacao;
    private Boolean estornavel;
}
