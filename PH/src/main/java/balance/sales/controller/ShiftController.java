package balance.sales.controller;

import balance.sales.dto.ShiftEditDTO;
import balance.sales.dto.ShiftResponseDTO;
import balance.sales.service.ShiftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2")
public class ShiftController {

    @Autowired
    private ShiftService shiftService;

    // Abrir turno
    @PostMapping("/stores/{storeId}/shifts")
    public ResponseEntity<?> openShift(@PathVariable Long storeId,
                                        @RequestBody Map<String, Object> body) {
        try {
            String username = body.getOrDefault("username", "unknown").toString();
            java.math.BigDecimal openingCash = null;
            if (body.get("openingCashAmount") != null) {
                openingCash = new java.math.BigDecimal(body.get("openingCashAmount").toString());
            }
            return ResponseEntity.ok(shiftService.openShift(storeId, username, openingCash));
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

    // Turno activo de un usuario en un local
    @GetMapping("/shifts/active/{storeId}")
    public ResponseEntity<?> getActiveShift(@PathVariable Long storeId,
                                             @RequestParam String username) {
        ShiftResponseDTO dto = shiftService.getActiveShift(storeId, username);
        if (dto == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(dto);
    }

    // Turno abierto de un usuario en cualquier local (fallback para admin)
    @GetMapping("/shifts/active")
    public ResponseEntity<?> getActiveShiftByUser(@RequestParam String username) {
        ShiftResponseDTO dto = shiftService.getActiveShiftByUsername(username);
        if (dto == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(dto);
    }

    // Historial de turnos de un local — filtros opcionales: username, from, to (yyyy-MM-dd)
    @GetMapping("/stores/{storeId}/shifts")
    public ResponseEntity<List<ShiftResponseDTO>> getHistory(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(shiftService.getShiftHistory(storeId, username, from, to, page, size));
    }

    // "Mis ventas": historial de un usuario en todos los locales
    @GetMapping("/shifts")
    public ResponseEntity<List<ShiftResponseDTO>> getByUsername(
            @RequestParam String username,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(shiftService.getShiftsByUsername(username, page, size));
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

    // Editar valores de un cierre
    @PutMapping("/shifts/{shiftId}/edit")
    public ResponseEntity<?> editShift(@PathVariable Long shiftId,
                                        @RequestBody ShiftEditDTO dto) {
        try {
            return ResponseEntity.ok(shiftService.editShift(shiftId, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo editar el turno: " + e.getMessage()));
        }
    }

    // Eliminar turno con todos sus datos (ventas, ítems, cierre de caja)
    @DeleteMapping("/shifts/{shiftId}")
    public ResponseEntity<?> deleteShift(@PathVariable Long shiftId) {
        try {
            shiftService.deleteShift(shiftId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo eliminar el turno: " + e.getMessage()));
        }
    }
}

