package br.eti.logos.controller.publico;

import br.eti.logos.dto.auth.AuthResponseDto;
import br.eti.logos.dto.auth.LoginRequestDto;
import br.eti.logos.feign.WsSecurityFeign;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class LoginController {

    private final WsSecurityFeign wsSecurityFeign;

    @PostMapping
    public ResponseEntity<AuthResponseDto> login(@RequestBody @Valid LoginRequestDto request) {
        return ResponseEntity.ok(wsSecurityFeign.login(request));
    }
}
