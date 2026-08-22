package balance.deposit.controller;

import balance.deposit.dto.CreateDepositRequest;
import balance.deposit.dto.DepositResponse;
import balance.deposit.dto.PendingClosingResponse;
import balance.deposit.service.BankDepositService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v2/deposits")
public class BankDepositController {

    @Autowired private BankDepositService service;

    // GET /api/v2/deposits/pending-closings?storeId=1&from=2026-05-17&to=2026-05-20
    @GetMapping("/pending-closings")
    public ResponseEntity<List<PendingClosingResponse>> getPendingClosings(
            @RequestParam Long storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.getPendingClosings(storeId, from, to));
    }

    // POST /api/v2/deposits
    @PostMapping
    public ResponseEntity<DepositResponse> create(
            @RequestParam String username,
            @Valid @RequestBody CreateDepositRequest req) {
        try {
            return ResponseEntity.ok(service.createDeposit(username, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // GET /api/v2/deposits?status=DISCREPANCY&page=0&size=20
    @GetMapping
    public ResponseEntity<Page<DepositResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getAll(status, page, size));
    }

    // GET /api/v2/deposits/{id}
    @GetMapping("/{id}")
    public ResponseEntity<DepositResponse> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // PUT /api/v2/deposits/{id}/confirm
    @PutMapping("/{id}/confirm")
    public ResponseEntity<DepositResponse> confirm(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.confirmDeposit(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/v2/deposits/mine?username=gimena_alba
    @GetMapping("/mine")
    public ResponseEntity<List<DepositResponse>> getMine(@RequestParam String username) {
        return ResponseEntity.ok(service.getByUser(username));
    }

    // POST /api/v2/deposits/extraordinary
    // Crea un depósito extraordinario directo (admin only): amount + storeId + depositDate
    // No requiere cierres pendientes — genera el ClosingDeposit y BankDeposit en una sola operación.
    @PostMapping("/extraordinary")
    public ResponseEntity<DepositResponse> createExtraordinary(
            @RequestParam String username,
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate depositDate,
            @RequestParam BigDecimal amount) {
        try {
            return ResponseEntity.ok(service.createExtraordinaryDeposit(username, storeId, depositDate, amount));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // DELETE /api/v2/deposits/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.deleteDeposit(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
