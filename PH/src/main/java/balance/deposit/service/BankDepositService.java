package balance.deposit.service;

import balance.deposit.dto.CreateDepositRequest;
import balance.deposit.dto.DepositResponse;
import balance.deposit.dto.PendingClosingResponse;
import balance.deposit.dto.UpdateDepositRequest;
import balance.deposit.model.BankDeposit;
import balance.deposit.repository.BankDepositRepository;
import balance.model.ClosingDeposit;
import balance.repository.ClosingDepositRepository;
import balance.sales.model.Sale;
import balance.sales.repository.SaleRepository;
import balance.sales.repository.ShiftRepository;
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
    @Autowired private SaleRepository saleRepository;
    @Autowired private ShiftRepository shiftRepository;

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

        // Marcar los cierres como depositados y propagar la imagen del comprobante via SQL directo
        for (ClosingDeposit c : closings) {
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

    // Eliminar depósito bancario en cascada (cierres, turnos, ventas)
    @Transactional
    public void deleteDeposit(Long id) {
        BankDeposit deposit = depositRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Depósito no encontrado"));

        List<ClosingDeposit> closings = closingDepositRepo.findByBankDepositId(id);
        for (ClosingDeposit c : closings) {
            Long shiftId = c.getShiftId();
            if (shiftId != null) {
                List<Sale> sales = saleRepository.findByShiftIdOrderByCreatedAtDesc(shiftId);
                if (!sales.isEmpty()) saleRepository.deleteAll(sales);
                if (shiftRepository.existsById(shiftId)) shiftRepository.deleteById(shiftId);
            }
        }
        closingDepositRepo.deleteAll(closings);
        depositRepo.delete(deposit);
    }

    // Editar depósito bancario (fecha, monto, imagen, notas)
    @Transactional
    public DepositResponse updateDeposit(Long id, UpdateDepositRequest req) {
        BankDeposit deposit = depositRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Depósito no encontrado"));

        if (req.getDepositDate() != null) deposit.setDepositDate(req.getDepositDate());
        if (req.getDeclaredAmount() != null) {
            deposit.setDeclaredAmount(req.getDeclaredAmount());
            BigDecimal difference = req.getDeclaredAmount().subtract(deposit.getExpectedCash());
            deposit.setDifference(difference);
            deposit.setStatus(difference.compareTo(BigDecimal.ZERO) == 0 ? "CONFIRMED" : "DISCREPANCY");
        }
        if (req.getImageUri() != null) deposit.setImageUri(req.getImageUri());
        if (req.getNotes() != null) deposit.setNotes(req.getNotes());

        return DepositResponse.from(depositRepo.save(deposit));
    }
}
