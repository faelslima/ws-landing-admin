package br.eti.logos.service.audit;

import jakarta.servlet.http.HttpServletRequest;

public interface LogErroService {

    void registrar(Exception e, HttpServletRequest request);
}
