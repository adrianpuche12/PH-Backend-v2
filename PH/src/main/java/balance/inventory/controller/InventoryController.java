package balance.inventory.controller;

import balance.inventory.dto.*;
import balance.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2")
@CrossOrigin(origins = "*")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/stores/{storeId}/stock")
    @PreAuthorize("hasAnyRole('admin', 'user')")
    public ResponseEntity<List<StockItemDTO>> getStock(@PathVariable Long storeId) {
        return ResponseEntity.ok(inventoryService.getStock(storeId));
    }

    @GetMapping("/stores/{storeId}/stock/low")
    @PreAuthorize("hasAnyRole('admin', 'user')")
    public ResponseEntity<List<StockItemDTO>> getLowStock(@PathVariable Long storeId) {
        return ResponseEntity.ok(inventoryService.getLowStock(storeId));
    }

    @GetMapping("/stores/{storeId}/stock/summary")
    @PreAuthorize("hasAnyRole('admin', 'user')")
    public ResponseEntity<StockSummaryDTO> getSummary(@PathVariable Long storeId) {
        return ResponseEntity.ok(inventoryService.getSummary(storeId));
    }

    // Solo admin puede crear ajustes manuales (SALIDA/AJUSTE); user solo ENTRADA
    @PostMapping("/stores/{storeId}/stock/adjustment")
    @PreAuthorize("hasAnyRole('admin', 'user')")
    public ResponseEntity<?> adjust(@PathVariable Long storeId,
                                     @Valid @RequestBody StockAdjustmentDTO dto) {
        try {
            return ResponseEntity.ok(inventoryService.adjust(storeId, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stores/{storeId}/stock/movements")
    @PreAuthorize("hasAnyRole('admin', 'user')")
    public ResponseEntity<List<MovementDTO>> getMovements(@PathVariable Long storeId) {
        return ResponseEntity.ok(inventoryService.getMovements(storeId));
    }
}
