package br.eti.logos.controller.admin;

import br.eti.logos.dto.request.PlanoCreateRequestDto;
import br.eti.logos.dto.response.PlanoResponseDto;
import br.eti.logos.service.plano.PlanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/planos")
@RequiredArgsConstructor
public class AdminPlanoApi {

    private final PlanoService planoService;

    @GetMapping

    public ResponseEntity<List<PlanoResponseDto>> listar() {
        return ResponseEntity.ok(planoService.listarTodos());
    }

    @PostMapping

    public ResponseEntity<PlanoResponseDto> criar(@RequestBody @Valid PlanoCreateRequestDto request) {
        return ResponseEntity.ok(planoService.criar(request));
    }

    @PutMapping("/{id}")

    public ResponseEntity<PlanoResponseDto> atualizar(@PathVariable UUID id, @RequestBody PlanoCreateRequestDto request) {
        return ResponseEntity.ok(planoService.atualizar(id, request));
    }

    @PostMapping("/{id}/sincronizar-pagbank")

    public ResponseEntity<Void> sincronizarPagBank(@PathVariable UUID id) {
        planoService.sincronizarComPagBank(id);
        return ResponseEntity.ok().build();
    }
}
