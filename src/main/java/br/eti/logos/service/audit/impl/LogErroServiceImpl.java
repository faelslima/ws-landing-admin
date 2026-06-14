package br.eti.logos.service.audit.impl;

import br.eti.logos.entity.audit.LogErro;
import br.eti.logos.repository.LogErroRepository;
import br.eti.logos.service.audit.LogErroService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogErroServiceImpl implements LogErroService {

    private final LogErroRepository logErroRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Exception e, HttpServletRequest request) {
        try {
            logErroRepository.save(LogErro.builder()
                    .endpoint(request != null ? request.getRequestURI() : "unknown")
                    .httpMethod(request != null ? request.getMethod() : "unknown")
                    .dataHora(LocalDateTime.now())
                    .stackTrace(toStackTrace(e))
                    .build());
        } catch (Exception ex) {
            log.error("Falha ao persistir log de erro: {}", ex.getMessage());
        }
    }

    private String toStackTrace(Exception e) {
        var sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
