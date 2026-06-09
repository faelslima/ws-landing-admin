package br.eti.logos.service.auth.impl;

import br.eti.logos.dto.auth.AuthResponseDto;
import br.eti.logos.dto.auth.LoginRequestDto;
import br.eti.logos.feign.WsSecurityFeign;
import br.eti.logos.service.auth.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private static final String ROLE_REQUIRED = "I12_GESTAO_VENDAS";

    private final WsSecurityFeign wsSecurityFeign;

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        AuthResponseDto auth = wsSecurityFeign.login(request);

        var roles = auth.getUser() != null ? auth.getUser().getRoles() : null;
        if (roles == null || !roles.contains(ROLE_REQUIRED)) {
            invalidarSessaoSilencioso(auth.getAccessToken());
            throw new SecurityException("Usuário não possui permissão para acessar este sistema");
        }

        return auth;
    }

    private void invalidarSessaoSilencioso(String token) {
        if (token == null) return;
        try {
            wsSecurityFeign.invalidarSessao(token);
        } catch (Exception e) {
            log.warn("Não foi possível invalidar sessão após rejeição de acesso: {}", e.getMessage());
        }
    }
}
