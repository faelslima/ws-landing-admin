package br.eti.logos.controller.publico;

import br.eti.logos.dto.request.CheckoutRequestDto;
import br.eti.logos.dto.request.LeadRequestDto;
import br.eti.logos.dto.response.PlanoResponseDto;
import br.eti.logos.service.onboarding.OnboardingService;
import br.eti.logos.service.plano.PlanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicLeadApi {

    private final OnboardingService onboardingService;
    private final PlanoService planoService;

    @PostMapping("/leads")
    public ResponseEntity<Void> registrarLead(@RequestBody @Valid LeadRequestDto request) {
        onboardingService.registrarLead(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/planos")
    public ResponseEntity<List<PlanoResponseDto>> listarPlanos() {
        return ResponseEntity.ok(planoService.listarPlanosAtivos());
    }

    @PostMapping("/checkout")
    public ResponseEntity<Void> checkout(@RequestBody @Valid CheckoutRequestDto request) {
        onboardingService.iniciarCheckout(request);
        return ResponseEntity.noContent().build();
    }
}
