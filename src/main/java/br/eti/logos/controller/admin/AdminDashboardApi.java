package br.eti.logos.controller.admin;

import br.eti.logos.dto.response.DashboardDto;
import br.eti.logos.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardApi {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('I12_GESTAO_VENDAS')")
    public ResponseEntity<DashboardDto> dashboard() {
        return ResponseEntity.ok(dashboardService.obterDashboard());
    }
}
