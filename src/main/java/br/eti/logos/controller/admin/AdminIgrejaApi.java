package br.eti.logos.controller.admin;

import br.eti.logos.dto.request.IgrejaManualRequestDto;
import br.eti.logos.dto.response.IgrejaResponseDto;
import br.eti.logos.service.igreja.AdminIgrejaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/igrejas")
@RequiredArgsConstructor
public class AdminIgrejaApi {

    private final AdminIgrejaService adminIgrejaService;

    @GetMapping
    public ResponseEntity<Page<IgrejaResponseDto>> listar(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) Boolean ativo,
            Pageable pageable) {
        return ResponseEntity.ok(adminIgrejaService.listar(termo, ativo, pageable));
    }

    @GetMapping("/{igrejaId}")
    public ResponseEntity<IgrejaResponseDto> buscar(@PathVariable String igrejaId) {
        return ResponseEntity.ok(adminIgrejaService.buscar(igrejaId));
    }

    @PostMapping
    public ResponseEntity<IgrejaResponseDto> criar(@Valid @RequestBody IgrejaManualRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminIgrejaService.criar(request));
    }

    @PutMapping("/{igrejaId}/inativar")
    public ResponseEntity<Void> inativar(@PathVariable String igrejaId, @RequestBody Map<String, String> body) {
        adminIgrejaService.inativar(igrejaId, body.get("motivo"));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{igrejaId}/reativar")
    public ResponseEntity<Void> reativar(@PathVariable String igrejaId) {
        adminIgrejaService.reativar(igrejaId);
        return ResponseEntity.ok().build();
    }
}
