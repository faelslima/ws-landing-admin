package br.eti.logos.feign;

import br.eti.logos.dto.auth.AuthResponseDto;
import br.eti.logos.dto.auth.CriarUsuarioRequestDto;
import br.eti.logos.dto.auth.LoginRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ws-security", url = "${api.security.url}")
public interface WsSecurityFeign {

    @PostMapping("/login")
    AuthResponseDto login(@RequestBody LoginRequestDto credentials);

    @PostMapping("/login/create-and-auth")
    AuthResponseDto criarUsuario(@RequestBody CriarUsuarioRequestDto request);

    @PutMapping("/session/invalidate/{token}")
    void invalidarSessao(@PathVariable("token") String token);
}
