package balance.service;

import balance.dto.AllOperationsDTO;
import balance.model.ClosingDeposit;
import balance.model.SupplierPayment;
import balance.model.SalaryPayment;
import balance.repository.ClosingDepositRepository;
import balance.repository.SupplierPaymentRepository;
import balance.repository.SalaryPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.math.BigDecimal;
import java.util.ArrayList;
import balance.model.Store;
import balance.dto.GastoAdminRequestDTO;
import balance.dto.GastoAdminResponseDTO;
import balance.model.GastoAdmin;
import balance.model.Transaction;
import balance.repository.GastoAdminRepository;
import balance.repository.TransactionRepository;
import balance.repository.StoreRepository;
import balance.sales.model.Shift;
import balance.sales.repository.ShiftRepository;
import balance.sales.repository.SaleRepository;


@Service
@Transactional
public class FormsService {

    private static final ZoneId HONDURAS_TZ = ZoneId.of("America/Tegucigalpa");

    @Autowired
    private GastoAdminRepository gastoAdminRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ClosingDepositRepository closingDepositRepository;

    @Autowired
    private SupplierPaymentRepository supplierPaymentRepository;

    @Autowired
    private SalaryPaymentRepository salaryPaymentRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private balance.storage.service.R2StorageService r2StorageService;

    @Autowired
    private balance.deposit.repository.BankDepositRepository bankDepositRepository;

    // ── Helper: enriquece los CLOSING DTOs con datos del turno vinculado ────────
    // Usa batch queries para evitar N+1: 2 queries totales en lugar de 2 por cada cierre.

    private void enrichClosingDTOs(List<AllOperationsDTO> dtos) {
        List<Long> shiftIds = dtos.stream()
            .filter(dto -> "CLOSING".equals(dto.getType()) && dto.getClosingShiftId() != null)
            .map(AllOperationsDTO::getClosingShiftId)
            .collect(Collectors.toList());

        if (shiftIds.isEmpty()) return;

        Map<Long, Shift> shiftMap = shiftRepository.findAllById(shiftIds).stream()
            .collect(Collectors.toMap(Shift::getId, s -> s));

        Map<Long, Long> countsMap = saleRepository.countByShiftIdIn(shiftIds).stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (Long) row[1]
            ));

        dtos.stream()
            .filter(dto -> "CLOSING".equals(dto.getType()) && dto.getClosingShiftId() != null)
            .forEach(dto -> {
                Shift shift = shiftMap.get(dto.getClosingShiftId());
                if (shift != null) {
                    int count = countsMap.getOrDefault(dto.getClosingShiftId(), 0L).intValue();
                    dto.enrichWithShift(shift, count);
                }
            });

        // Enriquecer con las notas del depósito bancario (batch: 1 query)
        List<Long> bankDepositIds = dtos.stream()
            .filter(dto -> "CLOSING".equals(dto.getType()) && dto.getBankDepositId() != null)
            .map(AllOperationsDTO::getBankDepositId)
            .distinct()
            .collect(Collectors.toList());

        if (!bankDepositIds.isEmpty()) {
            Map<Long, String> notesMap = bankDepositRepository.findAllById(bankDepositIds).stream()
                .filter(bd -> bd.getNotes() != null && !bd.getNotes().isBlank())
                .collect(Collectors.toMap(
                    balance.deposit.model.BankDeposit::getId,
                    balance.deposit.model.BankDeposit::getNotes
                ));
            dtos.stream()
                .filter(dto -> "CLOSING".equals(dto.getType()) && dto.getBankDepositId() != null)
                .forEach(dto -> dto.setBankDepositNotes(notesMap.get(dto.getBankDepositId())));
        }
    }

    // Métodos para obtener operaciones
    public List<AllOperationsDTO> getAllOperations() {
        // Sin filtro de fecha: limitar a los últimos 90 días para evitar traer toda la historia.
        // El usuario puede usar el filtro de fechas para ver períodos anteriores.
        LocalDate endDate   = LocalDate.now(HONDURAS_TZ);
        LocalDate startDate = endDate.minusDays(90);
        return getOperationsByDateRange(startDate, endDate);
    }

    public List<AllOperationsDTO> getOperationsByDateRange(LocalDate startDate, LocalDate endDate) {
        List<AllOperationsDTO> allOperations = new ArrayList<>();

        closingDepositRepository.findByDepositDateBetweenOrderByDepositDateDesc(startDate, endDate)
                .stream()
                .map(AllOperationsDTO::fromClosingDeposit)
                .forEach(allOperations::add);

        supplierPaymentRepository.findByPaymentDateBetweenOrderByPaymentDateDesc(startDate, endDate)
                .stream()
                .map(AllOperationsDTO::fromSupplierPayment)
                .forEach(allOperations::add);

        salaryPaymentRepository.findBySalaryDateBetweenOrderBySalaryDateDesc(startDate, endDate)
                .stream()
                .map(AllOperationsDTO::fromSalaryPayment)
                .forEach(allOperations::add);

        enrichClosingDTOs(allOperations);
        allOperations.sort((o1, o2) -> {
            if (o1.getDate() == null) return 1;
            if (o2.getDate() == null) return -1;
            return o2.getDate().compareTo(o1.getDate());
        });

        return allOperations;
    }

    // Método para filtrar por store — mismo límite de 90 días que getAllOperations()
    public List<AllOperationsDTO> getOperationsByStore(Long storeId) {
        LocalDate endDate   = LocalDate.now(HONDURAS_TZ);
        LocalDate startDate = endDate.minusDays(90);
        return getOperationsByDateRangeAndStore(startDate, endDate, storeId);
    }

    // Método para filtrar por fecha y store
    public List<AllOperationsDTO> getOperationsByDateRangeAndStore(LocalDate startDate, LocalDate endDate, Long storeId) {
        List<AllOperationsDTO> allOperations = new ArrayList<>();

        closingDepositRepository.findByDepositDateBetweenAndStoreId(startDate, endDate, storeId)
                .stream()
                .map(AllOperationsDTO::fromClosingDeposit)
                .forEach(allOperations::add);

        supplierPaymentRepository.findByPaymentDateBetweenAndStoreId(startDate, endDate, storeId)
                .stream()
                .map(AllOperationsDTO::fromSupplierPayment)
                .forEach(allOperations::add);

        salaryPaymentRepository.findBySalaryDateBetweenAndStoreId(startDate, endDate, storeId)
                .stream()
                .map(AllOperationsDTO::fromSalaryPayment)
                .forEach(allOperations::add);

        enrichClosingDTOs(allOperations);
        allOperations.sort((o1, o2) -> {
            if (o1.getDate() == null) return 1;
            if (o2.getDate() == null) return -1;
            return o2.getDate().compareTo(o1.getDate());
        });

        return allOperations;
    }

    // Métodos para guardar operaciones
    public ClosingDeposit saveClosingDeposit(ClosingDeposit deposit) {
        if (deposit.getDepositDate() == null) {
            deposit.setDepositDate(LocalDate.now(HONDURAS_TZ));
        }
        return closingDepositRepository.save(deposit);
    }

    public List<ClosingDeposit> getClosingDeposits(LocalDate startDate, LocalDate endDate) {
        return closingDepositRepository.findByDepositDateBetweenOrderByDepositDateDesc(startDate, endDate);
    }

    public List<ClosingDeposit> getAllClosingDeposits() {
        return closingDepositRepository.findAllOrderByDepositDateDesc();
    }

    public void updateClosingDepositImage(Long id, String newImageUri) {
        ClosingDeposit closing = closingDepositRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Cierre no encontrado"));
        if (!"PENDING".equals(closing.getDepositStatus())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Solo se puede actualizar la imagen de un cierre pendiente");
        }
        if (closing.getImageUri() != null) {
            r2StorageService.delete(closing.getImageUri());
        }
        closingDepositRepository.updateImageUri(id, newImageUri);
    }

    public SupplierPayment saveSupplierPayment(SupplierPayment payment) {
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDate.now(HONDURAS_TZ));
        }
        return supplierPaymentRepository.save(payment);
    }

    public List<SupplierPayment> getSupplierPayments(LocalDate startDate, LocalDate endDate) {
        return supplierPaymentRepository.findByPaymentDateBetweenOrderByPaymentDateDesc(startDate, endDate);
    }

    public List<SupplierPayment> getAllSupplierPayments() {
        return supplierPaymentRepository.findAllOrderByPaymentDateDesc();
    }

    public SalaryPayment saveSalaryPayment(SalaryPayment payment) {
        if (payment.getSalaryDate() == null) {
            payment.setSalaryDate(LocalDate.now(HONDURAS_TZ));
        }
        return salaryPaymentRepository.save(payment);
    }

    public List<SalaryPayment> getAllSalaryPayments() {
        return salaryPaymentRepository.findAllOrderBySalaryDateDesc();
    }

    // 🔍 Filtro por store ID
    public List<ClosingDeposit> findByStoreId(Long storeId) {
        return closingDepositRepository.findByStoreId(storeId);
    }



    // Métodos de actualización (PUT)
    public ClosingDeposit updateClosingDeposit(Long id, ClosingDeposit updatedDeposit) {
        ClosingDeposit existingDeposit = closingDepositRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ClosingDeposit no encontrado con id " + id));
        existingDeposit.setAmount(updatedDeposit.getAmount());
        existingDeposit.setUsername(updatedDeposit.getUsername());
        
        if (updatedDeposit.getClosingsCount() != null) {
            existingDeposit.setClosingsCount(updatedDeposit.getClosingsCount());
        }
        
        if (updatedDeposit.getPeriodStart() != null) {
            existingDeposit.setPeriodStart(updatedDeposit.getPeriodStart());
        }
        
        if (updatedDeposit.getPeriodEnd() != null) {
            existingDeposit.setPeriodEnd(updatedDeposit.getPeriodEnd());
        }
        
        if (updatedDeposit.getDepositDate() != null) {
            existingDeposit.setDepositDate(updatedDeposit.getDepositDate());
        }
        if (updatedDeposit.getStore() != null) {
            existingDeposit.setStore(updatedDeposit.getStore());
        }
        return closingDepositRepository.save(existingDeposit);
    }

    public SupplierPayment updateSupplierPayment(Long id, SupplierPayment updatedPayment) {
        SupplierPayment existingPayment = supplierPaymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SupplierPayment no encontrado con id " + id));
        existingPayment.setAmount(updatedPayment.getAmount());
        if (updatedPayment.getDescription() != null)   existingPayment.setDescription(updatedPayment.getDescription());
        if (updatedPayment.getUsername()    != null)   existingPayment.setUsername(updatedPayment.getUsername());
        if (updatedPayment.getSupplier()    != null)   existingPayment.setSupplier(updatedPayment.getSupplier());
        if (updatedPayment.getPaymentDate() != null)   existingPayment.setPaymentDate(updatedPayment.getPaymentDate());
        if (updatedPayment.getStore()       != null)   existingPayment.setStore(updatedPayment.getStore());
        return supplierPaymentRepository.save(existingPayment);
    }

    public SalaryPayment updateSalaryPayment(Long id, SalaryPayment updatedPayment) {
        SalaryPayment existingPayment = salaryPaymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SalaryPayment no encontrado con id " + id));
        existingPayment.setAmount(updatedPayment.getAmount());
        if (updatedPayment.getDescription() != null) existingPayment.setDescription(updatedPayment.getDescription());
        if (updatedPayment.getUsername()    != null) existingPayment.setUsername(updatedPayment.getUsername());
        if (updatedPayment.getSalaryDate()  != null) existingPayment.setSalaryDate(updatedPayment.getSalaryDate());
        if (updatedPayment.getStore()       != null) existingPayment.setStore(updatedPayment.getStore());
        return salaryPaymentRepository.save(existingPayment);
    }

    // Métodos de eliminación (DELETE)
    public void deleteClosingDeposit(Long id) {
        if (!closingDepositRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ClosingDeposit no encontrado con id " + id);
        }
        closingDepositRepository.deleteById(id);
    }

    public void deleteSupplierPayment(Long id) {
        if (!supplierPaymentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SupplierPayment no encontrado con id " + id);
        }
        supplierPaymentRepository.deleteById(id);
    }

    public void deleteSalaryPayment(Long id) {
        if (!salaryPaymentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SalaryPayment no encontrado con id " + id);
        }
        salaryPaymentRepository.deleteById(id);
    }

    public List<AllOperationsDTO> getOperationsByUsername(String username) {
        List<AllOperationsDTO> result = new ArrayList<>();
        closingDepositRepository.findByUsernameOrderByDepositDateDesc(username)
                .stream().map(AllOperationsDTO::fromClosingDeposit).forEach(result::add);
        supplierPaymentRepository.findByUsernameOrderByPaymentDateDesc(username)
                .stream().map(AllOperationsDTO::fromSupplierPayment).forEach(result::add);
        salaryPaymentRepository.findByUsernameOrderBySalaryDateDesc(username)
                .stream().map(AllOperationsDTO::fromSalaryPayment).forEach(result::add);
        result.sort((a, b) -> {
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });
        return result;
    }

    public GastoAdminResponseDTO saveGastoAdmin(GastoAdminRequestDTO request) {
        // Validar que los porcentajes sumen exactamente 100%
        if (!request.isValidPercentages()) {
            throw new IllegalArgumentException("Los porcentajes deben sumar exactamente 100%");
        }

        // 1. Guardar registro en tabla gasto_admin para auditoría
        // Los campos legacy porcentajeDanli/Paraiso y montoDanli/Paraiso se guardan
        // como 0 ya que en V2 la distribución real vive en las transacciones individuales
        GastoAdmin gastoAdmin = new GastoAdmin();
        gastoAdmin.setFecha(request.getFecha());
        gastoAdmin.setMonto(request.getMonto());
        gastoAdmin.setDescripcion(request.getDescripcion());
        gastoAdmin.setUsername("admin_user");
        gastoAdmin.setPorcentajeDanli(0);
        gastoAdmin.setPorcentajeParaiso(0);
        gastoAdmin.setMontoDanli(BigDecimal.ZERO);
        gastoAdmin.setMontoParaiso(BigDecimal.ZERO);
        GastoAdmin gastoAdminSaved = gastoAdminRepository.save(gastoAdmin);

        // 2. Crear una transacción por cada local en la distribución
        List<GastoAdminResponseDTO.TransaccionCreada> transaccionesCreadas = new ArrayList<>();

        for (GastoAdminRequestDTO.StoreDistribucion dist : request.getDistribuciones()) {
            Store store = storeRepository.findById(dist.getStoreId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Local no encontrado con id: " + dist.getStoreId()));

            BigDecimal montoLocal = calcularMonto(request.getMonto(), dist.getPorcentaje());

            Transaction tx = new Transaction();
            tx.setType(request.getTipo());
            tx.setAmount(montoLocal);
            tx.setDate(request.getFecha());
            tx.setDescription(String.format("%s (%s %d%%)",
                    request.getDescripcion(), store.getName(), dist.getPorcentaje()));
            tx.setStore(store);
            tx.setGastoAdminId(gastoAdminSaved.getId());

            Transaction saved = transactionRepository.save(tx);

            transaccionesCreadas.add(new GastoAdminResponseDTO.TransaccionCreada(
                    saved.getId(),
                    saved.getType(),
                    saved.getAmount(),
                    saved.getDate(),
                    saved.getDescription(),
                    store.getName(),
                    dist.getPorcentaje()
            ));
        }

        // 3. Construir respuesta
        return new GastoAdminResponseDTO(
                "Gasto administrativo creado exitosamente. Se crearon " + transaccionesCreadas.size() + " transacciones.",
                transaccionesCreadas.size(),
                request.getMonto(),
                transaccionesCreadas,
                gastoAdminSaved.getId()
        );
    }

    public List<GastoAdmin> getAllGastosAdmin() {
        return gastoAdminRepository.findAll();
    }


    public List<GastoAdmin> getGastosAdmin(LocalDate startDate, LocalDate endDate) {
        return gastoAdminRepository.findByFechaBetween(startDate, endDate);
    }


    public GastoAdmin updateGastoAdmin(Long id, GastoAdmin updatedGastoAdmin) {
        GastoAdmin existingGastoAdmin = gastoAdminRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "GastoAdmin no encontrado con id " + id));
        
        // Actualizar campos básicos
        if (updatedGastoAdmin.getMonto() != null) {
            existingGastoAdmin.setMonto(updatedGastoAdmin.getMonto());
        }
        
        if (updatedGastoAdmin.getDescripcion() != null) {
            existingGastoAdmin.setDescripcion(updatedGastoAdmin.getDescripcion());
        }
        
        if (updatedGastoAdmin.getFecha() != null) {
            existingGastoAdmin.setFecha(updatedGastoAdmin.getFecha());
        }
        
        if (updatedGastoAdmin.getUsername() != null) {
            existingGastoAdmin.setUsername(updatedGastoAdmin.getUsername());
        }
        
        GastoAdmin saved = gastoAdminRepository.save(existingGastoAdmin);
        return saved;
    }

    @org.springframework.transaction.annotation.Transactional
    public GastoAdminResponseDTO updateGastoAdminV2(Long id, GastoAdminRequestDTO request) {
        if (!request.isValidPercentages()) {
            throw new IllegalArgumentException("Los porcentajes deben sumar exactamente 100%");
        }

        GastoAdmin existing = gastoAdminRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "GastoAdmin no encontrado con id " + id));

        // 1. Eliminar transacciones derivadas anteriores
        transactionRepository.deleteByGastoAdminId(id);

        // 2. Actualizar el registro principal
        existing.setFecha(request.getFecha());
        existing.setMonto(request.getMonto());
        existing.setDescripcion(request.getDescripcion());
        gastoAdminRepository.save(existing);

        // 3. Crear nuevas transacciones con la nueva distribución
        List<GastoAdminResponseDTO.TransaccionCreada> transaccionesCreadas = new ArrayList<>();

        for (GastoAdminRequestDTO.StoreDistribucion dist : request.getDistribuciones()) {
            Store store = storeRepository.findById(dist.getStoreId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Local no encontrado con id: " + dist.getStoreId()));

            BigDecimal montoLocal = calcularMonto(request.getMonto(), dist.getPorcentaje());

            Transaction tx = new Transaction();
            tx.setType(request.getTipo());
            tx.setAmount(montoLocal);
            tx.setDate(request.getFecha());
            tx.setDescription(String.format("%s (%s %d%%)",
                    request.getDescripcion(), store.getName(), dist.getPorcentaje()));
            tx.setStore(store);
            tx.setGastoAdminId(id);

            Transaction saved = transactionRepository.save(tx);

            transaccionesCreadas.add(new GastoAdminResponseDTO.TransaccionCreada(
                    saved.getId(), saved.getType(), saved.getAmount(),
                    saved.getDate(), saved.getDescription(),
                    store.getName(), dist.getPorcentaje()
            ));
        }

        return new GastoAdminResponseDTO(
                "Gasto administrativo actualizado. Se recrearon " + transaccionesCreadas.size() + " transacciones.",
                transaccionesCreadas.size(),
                request.getMonto(),
                transaccionesCreadas,
                id
        );
    }

    public void deleteGastoAdmin(Long id) {
        if (!gastoAdminRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "GastoAdmin no encontrado con id " + id);
        }
        gastoAdminRepository.deleteById(id);
    }

    private BigDecimal calcularMonto(BigDecimal montoTotal, Integer porcentaje) {
        return montoTotal.multiply(BigDecimal.valueOf(porcentaje))
                        .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }
}