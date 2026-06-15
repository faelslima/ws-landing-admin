package br.eti.logos.dto.response;

import br.eti.logos.enums.FormaPagamentoEnum;
import br.eti.logos.enums.PagamentoStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoResponseDto implements Serializable {

    private UUID id;
    private String pagbankInvoiceId;
    private String nomeIgreja;
    private String nomeResponsavel;
    private String planoNome;
    private BigDecimal valor;
    private BigDecimal valorEstornado;
    private PagamentoStatusEnum status;
    private FormaPagamentoEnum formaPagamento;
    private String dataPagamento;  // ISO 8601
    private String dataVencimento; // ISO 8601
    private String dataCriacao;    // ISO 8601
    private Boolean estornavel;
    private String motivoRecusa;
}
