package balance.deposit.service;

import balance.deposit.dto.CreateDepositRequest;
import balance.deposit.dto.DepositResponse;
import balance.deposit.dto.PendingClosingResponse;
import balance.deposit.model.BankDeposit;
import balance.deposit.repository.BankDepositRepository;
import balance.model.ClosingDeposit;
import balance.repository.ClosingDepositRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BankDepositService {

    @Autowired private BankDepositRepository depositRepo;
    @Autowired private ClosingDepositRepository closingDepositRepo;
    @Autowired private balance.storage.service.R2StorageService r2StorageService;

    // Cierres PENDING de un local en un rango de fechas
    public List<PendingClosingResponse> getPendingClosings(Long storeId, LocalDate from, LocalDate to) {
        List<ClosingDeposit> closings;
        if (from != null && to != null) {
            closings = closingDepositRepo
                    .findByStoreIdAndDepositStatusAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualOrderByDepositDateDesc(
                            storeId, "PENDING", from, to);
        } else {
            closings = closingDepositRepo.findByStoreIdAndDepositStatusOrderByDepositDateDesc(storeId, "PENDING");
        }
        return closings.stream().map(PendingClosingResponse::from).collect(Collectors.toList());
    }

    // Crear depósito bancario a partir de ClosingDeposits seleccionados
    @Transactional
    public DepositResponse createDeposit(String username, CreateDepositRequest req) {
        List<Long> closingIds = req.getShiftIds(); // reutilizo el campo — son IDs de ClosingDeposit

        List<ClosingDeposit> closings = closingDepositRepo.findAllById(closingIds);

        if (closings.size() != closingIds.size()) {
            throw new IllegalArgumentException("Uno o más cierres no existen");
        }
        for (ClosingDeposit c : closings) {
            if ("DEPOSITED".equals(c.getDepositStatus())) {
                throw new IllegalArgumentException("El cierre del " + c.getDepositDate() + " ya fue depositado");
            }
        }

        // El efectivo esperado es la suma de los montos de los cierres
        // (el monto del ClosingDeposit ya refleja el efectivo recaudado en ese cierre)
        BigDecimal expectedCash = closings.stream()
                .map(ClosingDeposit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal difference = req.getDeclaredAmount().subtract(expectedCash);
        String status = difference.compareTo(BigDecimal.ZERO) == 0 ? "CONFIRMED" : "DISCREPANCY";

        // Store IDs únicos
        String storeIds = closings.stream()
                .filter(c -> c.getStore() != null)
                .map(c -> c.getStore().getId().toString())
                .distinct()
                .collect(Collectors.joining(","));

        String closingIdsCsv = closingIds.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        BankDeposit deposit = new BankDeposit();
        deposit.setCreatedBy(username);
        deposit.setDepositDate(req.getDepositDate());
        deposit.setStoreIds(storeIds);
        deposit.setShiftIds(closingIdsCsv);
        deposit.setExpectedCash(expectedCash);
        deposit.setDeclaredAmount(req.getDeclaredAmount());
        deposit.setDifference(difference);
        deposit.setStatus(status);
        deposit.setImageUri(req.getImageUri());
        deposit.setNotes(req.getNotes());

        BankDeposit saved = depositRepo.save(deposit);

        // Borrar fotos individuales de cierre de R2 y marcar cierres como depositados.
        // El imageUri del cierre pasa a ser el comprobante del depósito bancario,
        // para que el DEPOSIT_GROUP en el frontend pueda mostrar la miniatura.
        for (ClosingDeposit c : closings) {
            if (c.getImageUri() != null) {
                r2StorageService.delete(c.getImageUri());
            }
            closingDepositRepo.updateDepositInfo(c.getId(), "DEPOSITED", saved.getId(), saved.getImageUri());
        }

        return DepositResponse.from(saved);
    }

    // Confirmar depósito con discrepancia (solo admin)
    @Transactional
    public DepositResponse confirmDeposit(Long depositId) {
        BankDeposit deposit = depositRepo.findById(depositId)
                .orElseThrow(() -> new IllegalArgumentException("Depósito no encontrado"));
        deposit.setStatus("CONFIRMED");
        return DepositResponse.from(depositRepo.save(deposit));
    }

    // Listado paginado (admin)
    public Page<DepositResponse> getAll(String status, int page, int size) {
        PageRequest pr = PageRequest.of(page, size);
        if (status != null && !status.isBlank()) {
            return depositRepo.findByStatusOrderByCreatedAtDesc(status, pr).map(DepositResponse::from);
        }
        return depositRepo.findAllByOrderByCreatedAtDesc(pr).map(DepositResponse::from);
    }

    // Detalle de un depósito
    public DepositResponse getById(Long id) {
        return depositRepo.findById(id)
                .map(DepositResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Depósito no encontrado"));
    }

    // Historial del empleado
    public List<DepositResponse> getByUser(String username) {
        return depositRepo.findByCreatedByOrderByCreatedAtDesc(username)
                .stream().map(DepositResponse::from).collect(Collectors.toList());
    }

    // Eliminar depósito bancario y revertir cierres asociados a PENDING
    @Transactional
    public void deleteDeposit(Long depositId) {
        BankDeposit deposit = depositRepo.findById(depositId)
                .orElseThrow(() -> new IllegalArgumentException("Depósito no encontrado"));

        // Revertir todos los cierres asociados: PENDING, sin bankDepositId, sin imageUri
        closingDepositRepo.revertByBankDepositId(depositId);

        // Eliminar el comprobante del depósito de R2 si existe
        if (deposit.getImageUri() != null) {
            r2StorageService.delete(deposit.getImageUri());
        }

        depositRepo.delete(deposit);
    }
}
