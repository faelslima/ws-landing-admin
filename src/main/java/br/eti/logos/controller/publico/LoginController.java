package br.eti.logos.controller.publico;

import br.eti.logos.dto.auth.AuthResponseDto;
import br.eti.logos.dto.auth.LoginRequestDto;
import br.eti.logos.feign.WsSecurityFeign;
import br.eti.logos.service.auth.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    private final WsSecurityFeign wsSecurityFeign;

    @PostMapping
    public ResponseEntity<AuthResponseDto> login(@RequestBody @Valid LoginRequestDto request) {
        return ResponseEntity.ok(loginService.login(request));
    }

    @PutMapping("/session/invalidate/{token}")
    public ResponseEntity<Void> invalidarSessao(@PathVariable String token) {
        try {
            wsSecurityFeign.invalidarSessao(token);
        } catch (Exception ignored) {
            // best-effort: não falha o logout do cliente se ws-security estiver indisponível
        }
        return ResponseEntity.ok().build();
    }
}
