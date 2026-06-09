package br.eti.logos.service.auth;

import br.eti.logos.dto.auth.AuthResponseDto;
import br.eti.logos.dto.auth.LoginRequestDto;

public interface LoginService {
    AuthResponseDto login(LoginRequestDto request);
}
