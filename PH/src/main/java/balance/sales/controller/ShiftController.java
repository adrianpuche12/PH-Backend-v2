package balance.sales.controller;

import balance.sales.dto.ShiftResponseDTO;
import balance.sales.service.ShiftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2")
@CrossOrigin(origins = "*")
public class ShiftController {

    @Autowired
    private ShiftService shiftService;

    // Abrir turno
    @PostMapping("/stores/{storeId}/shifts")
    public ResponseEntity<?> openShift(@PathVariable Long storeId,
                                        @RequestBody Map<String, String> body) {
        try {
            String username = body.getOrDefault("username", "unknown");
            return ResponseEntity.ok(shiftService.openShift(storeId, username));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo abrir el turno: " + e.getMessage()));
        }
    }

    // Cerrar turno (solo cierra el turno, sin procesar ventas)
    @PutMapping("/shifts/{shiftId}/close")
    public ResponseEntity<?> closeShift(@PathVariable Long shiftId) {
        try {
            return ResponseEntity.ok(shiftService.closeShift(shiftId));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Turno activo de un local
    @GetMapping("/shifts/active/{storeId}")
    public ResponseEntity<?> getActiveShift(@PathVariable Long storeId) {
        ShiftResponseDTO dto = shiftService.getActiveShift(storeId);
        if (dto == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(dto);
    }

    // Historial de turnos de un local
    @GetMapping("/stores/{storeId}/shifts")
    public ResponseEntity<List<ShiftResponseDTO>> getHistory(@PathVariable Long storeId) {
        return ResponseEntity.ok(shiftService.getShiftHistory(storeId));
    }

    // Detalle de un turno
    @GetMapping("/shifts/{shiftId}")
    public ResponseEntity<?> getById(@PathVariable Long shiftId) {
        try {
            return ResponseEntity.ok(shiftService.getById(shiftId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
