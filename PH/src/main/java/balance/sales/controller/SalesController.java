package balance.sales.controller;

import balance.sales.dto.*;
import balance.sales.service.SalesService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2")
@CrossOrigin(origins = "*")
public class SalesController {

    @Autowired
    private SalesService salesService;

    // Registrar venta en un turno
    @PostMapping("/shifts/{shiftId}/sales")
    public ResponseEntity<?> createSale(@PathVariable Long shiftId,
                                         @Valid @RequestBody SaleRequestDTO request) {
        try {
            return ResponseEntity.ok(salesService.createSale(shiftId, request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Listar ventas de un turno
    @GetMapping("/shifts/{shiftId}/sales")
    public ResponseEntity<List<SaleResponseDTO>> getSalesByShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(salesService.getSalesByShift(shiftId));
    }

    // Detalle de una venta
    @GetMapping("/sales/{saleId}")
    public ResponseEntity<?> getSale(@PathVariable Long saleId) {
        try {
            return ResponseEntity.ok(salesService.getSaleById(saleId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Cancelar venta (revierte stock)
    @DeleteMapping("/sales/{saleId}")
    public ResponseEntity<?> cancelSale(@PathVariable Long saleId) {
        try {
            salesService.cancelSale(saleId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Resumen del turno (para vista de cierre)
    @GetMapping("/shifts/{shiftId}/summary")
    public ResponseEntity<?> getSummary(@PathVariable Long shiftId) {
        try {
            return ResponseEntity.ok(salesService.getDailySummary(shiftId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Confirmar cierre de turno (integra con sistema V1)
    @PostMapping("/shifts/{shiftId}/closing")
    public ResponseEntity<?> closeShift(@PathVariable Long shiftId,
                                         @RequestBody Map<String, String> body) {
        try {
            String username = body.getOrDefault("username", "unknown");
            return ResponseEntity.ok(salesService.closeShift(shiftId, username));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
