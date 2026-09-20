package balance.dashboard.controller;

import balance.dashboard.dto.DashboardDTO;
import balance.dashboard.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /** Resumen del sistema. Sin parámetros: todos los locales (admin). Con ?storeIds=1,2: filtrado por local (socio). */
    @GetMapping
    public ResponseEntity<DashboardDTO> getDashboard(
            @RequestParam(required = false) List<Long> storeIds) {
        return ResponseEntity.ok(dashboardService.getDashboard(storeIds));
    }
}

