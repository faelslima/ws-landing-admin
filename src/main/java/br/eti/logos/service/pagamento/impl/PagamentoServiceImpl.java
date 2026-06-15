package br.eti.logos.service.pagamento.impl;

import br.eti.logos.core.util.DateTimeUtil;
import br.eti.logos.core.util.MoneyUtil;
import br.eti.logos.dto.request.RefundRequestDto;
import br.eti.logos.dto.response.PagamentoResponseDto;
import br.eti.logos.entity.landing.Pagamento;
import br.eti.logos.enums.PagamentoStatusEnum;
import br.eti.logos.repository.AssinaturaRepository;
import br.eti.logos.repository.IgrejaRepository;
import br.eti.logos.repository.LeadRepository;
import br.eti.logos.repository.PagamentoRepository;
import br.eti.logos.service.pagamento.PagamentoService;
import br.eti.logos.service.pagbank.PagBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagamentoServiceImpl implements PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final IgrejaRepository igrejaRepository;
    private final LeadRepository leadRepository;
    private final PagBankService pagBankService;

    @Override
    @Transactional(readOnly = true)
    public Page<PagamentoResponseDto> listar(PagamentoStatusEnum status, Pageable pageable) {
        Page<Pagamento> page;
        if (status != null) {
            page = pagamentoRepository.findAllByStatus(status, pageable);
        } else {
            page = pagamentoRepository.findAll(pageable);
        }
        return page.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PagamentoResponseDto> listarPorIgreja(UUID igrejaId, Pageable pageable) {
        return pagamentoRepository.findAllByAssinaturaLicencaIgrejaId(igrejaId, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PagamentoResponseDto buscarPorId(UUID id) {
        return toDto(findById(id));
    }

    @Override
    @Transactional
    public void estornar(RefundRequestDto request) {
        var pagamento = findById(request.getPagamentoId());

        if (pagamento.getStatus() != PagamentoStatusEnum.PAID) {
            throw new IllegalStateException("Apenas pagamentos com status PAID podem ser estornados");
        }

        var estornoTotal = request.getValor() != null
                ? request.getValor()
                : pagamento.getValor();

        if (pagamento.getPagbankChargeId() != null) {
            pagBankService.cancelarCobranca(pagamento.getPagbankChargeId(), MoneyUtil.reaisParaCentavos(estornoTotal));
        }

        if (estornoTotal.compareTo(pagamento.getValor()) >= 0) {
            pagamento.setStatus(PagamentoStatusEnum.REFUNDED);
        } else {
            pagamento.setStatus(PagamentoStatusEnum.PARTIALLY_REFUNDED);
        }
        pagamento.setValorEstornado(pagamento.getValorEstornado().add(estornoTotal));
        pagamento.setDataEstorno(OffsetDateTime.now());
        pagamento.setMotivoEstorno(request.getMotivo());
        pagamentoRepository.save(pagamento);

        log.info("Pagamento estornado: {} - Valor: R$ {}", pagamento.getId(), estornoTotal);
    }

    @Override
    @Transactional
    public void cancelar(UUID pagamentoId, String motivo) {
        var pagamento = findById(pagamentoId);

        if (pagamento.getStatus() == PagamentoStatusEnum.PAID) {
            throw new IllegalStateException("Use estorno para pagamentos já confirmados");
        }

        pagamento.setStatus(PagamentoStatusEnum.CANCELED);
        pagamento.setMotivoEstorno(motivo);
        pagamentoRepository.save(pagamento);

        log.info("Pagamento cancelado: {}", pagamentoId);
    }

    @Override
    public void retentarCobranca(UUID pagamentoId) {
        var pagamento = findById(pagamentoId);

        if (pagamento.getPagbankInvoiceId() == null) {
            throw new IllegalStateException("Pagamento sem invoice PagBank vinculada");
        }

        pagBankService.retentarFatura(pagamento.getPagbankInvoiceId());
        log.info("Retentativa de cobrança solicitada: {}", pagamentoId);
    }

    private Pagamento findById(UUID id) {
        return pagamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado: " + id));
    }

    private PagamentoResponseDto toDto(Pagamento p) {
        var assinatura = p.getAssinatura();
        var licenca = assinatura.getLicenca();
        var igreja = igrejaRepository.findById(licenca.getIgrejaId()).orElse(null);
        var lead = leadRepository.findTopByIgrejaIdConvertidaOrderByCriadoEmDesc(licenca.getIgrejaId()).orElse(null);

        var nomeIgreja = igreja != null ? igreja.getRazaoSocial()
                : (lead != null ? lead.getNomeIgreja() : "N/A");
        var nomeResponsavel = igreja != null ? igreja.getNomeResponsavel()
                : (lead != null ? lead.getNomeResponsavel() : null);

        var estornavel = p.getStatus() == PagamentoStatusEnum.PAID
                && p.getValorEstornado().compareTo(p.getValor()) < 0;

        return PagamentoResponseDto.builder()
                .id(p.getId())
                .pagbankInvoiceId(p.getPagbankInvoiceId())
                .nomeIgreja(nomeIgreja)
                .nomeResponsavel(nomeResponsavel)
                .planoNome(licenca.getPlano().getNome())
                .valor(p.getValor())
                .valorEstornado(p.getValorEstornado())
                .status(p.getStatus())
                .formaPagamento(p.getFormaPagamento())
                .dataPagamento(DateTimeUtil.toIsoString(p.getDataPagamento()))
                .dataVencimento(DateTimeUtil.toIsoString(p.getDataVencimento()))
                .dataCriacao(DateTimeUtil.toIsoString(p.getCriadoEm()))
                .estornavel(estornavel)
                .motivoRecusa(p.getMotivoRecusa())
                .build();
    }
}
