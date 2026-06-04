package br.eti.logos.controller.publico;

import br.eti.logos.dto.request.CheckoutRequestDto;
import br.eti.logos.dto.request.LeadRequestDto;
import br.eti.logos.dto.response.PlanoResponseDto;
import br.eti.logos.entity.landing.Plano;
import br.eti.logos.repository.PlanoRepository;
import br.eti.logos.service.onboarding.OnboardingService;
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
    private final PlanoRepository planoRepository;

    @PostMapping("/leads")
    public ResponseEntity<Void> registrarLead(@RequestBody @Valid LeadRequestDto request) {
        onboardingService.registrarLead(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/planos")
    public ResponseEntity<List<PlanoResponseDto>> listarPlanos() {
        var planos = planoRepository.findAllByAtivoTrue().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(planos);
    }

    @PostMapping("/checkout")
    public ResponseEntity<String> checkout(@RequestBody @Valid CheckoutRequestDto request) {
        var subscriptionId = onboardingService.iniciarCheckout(request);
        return ResponseEntity.ok(subscriptionId);
    }

    private PlanoResponseDto toDto(Plano p) {
        return PlanoResponseDto.builder()
                .id(p.getId())
                .nome(p.getNome())
                .descricao(p.getDescricao())
                .tier(p.getTier())
                .limiteUsuarios(p.getLimiteUsuarios())
                .valorAnualCentavos(p.getValorAnualCentavos())
                .ativo(p.getAtivo())
                .recursos(p.getRecursos())
                .build();
    }
}
