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
import java.util.ArrayList;
import java.util.List;
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


@Service
@Transactional
public class FormsService {

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

    // Métodos para obtener operaciones
    public List<AllOperationsDTO> getAllOperations() {
        List<AllOperationsDTO> allOperations = new ArrayList<>();

        allOperations.addAll(
                closingDepositRepository.findAll().stream()
                        .map(AllOperationsDTO::fromClosingDeposit)
                        .collect(Collectors.toList())
        );

        allOperations.addAll(
                supplierPaymentRepository.findAll().stream()
                        .map(AllOperationsDTO::fromSupplierPayment)
                        .collect(Collectors.toList())
        );

        allOperations.addAll(
                salaryPaymentRepository.findAll().stream()
                        .map(AllOperationsDTO::fromSalaryPayment)
                        .collect(Collectors.toList())
        );

        allOperations.sort((t1, t2) -> {
            if (t1.getDate() == null) return 1;
            if (t2.getDate() == null) return -1;
            return t2.getDate().compareTo(t1.getDate());
        });

        return allOperations;
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

        allOperations.sort((o1, o2) -> {
            if (o1.getDate() == null) return 1;
            if (o2.getDate() == null) return -1;
            return o2.getDate().compareTo(o1.getDate());
        });

        return allOperations;
    }

    // Método para filtrar por store
    public List<AllOperationsDTO> getOperationsByStore(Long storeId) {
        List<AllOperationsDTO> allOperations = new ArrayList<>();

        closingDepositRepository.findByStoreId(storeId)
                .stream()
                .map(AllOperationsDTO::fromClosingDeposit)
                .forEach(allOperations::add);

        supplierPaymentRepository.findByStoreId(storeId)
                .stream()
                .map(AllOperationsDTO::fromSupplierPayment)
                .forEach(allOperations::add);

        salaryPaymentRepository.findByStoreId(storeId)
                .stream()
                .map(AllOperationsDTO::fromSalaryPayment)
                .forEach(allOperations::add);

        allOperations.sort((o1, o2) -> {
            if (o1.getDate() == null) return 1;
            if (o2.getDate() == null) return -1;
            return o2.getDate().compareTo(o1.getDate());
        });

        return allOperations;
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
            deposit.setDepositDate(LocalDate.now());
        }
        return closingDepositRepository.save(deposit);
    }

    public List<ClosingDeposit> getClosingDeposits(LocalDate startDate, LocalDate endDate) {
        return closingDepositRepository.findByDepositDateBetweenOrderByDepositDateDesc(startDate, endDate);
    }

    public List<ClosingDeposit> getAllClosingDeposits() {
        return closingDepositRepository.findAllOrderByDepositDateDesc();
    }



    public SupplierPayment saveSupplierPayment(SupplierPayment payment) {
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDate.now());
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
            payment.setSalaryDate(LocalDate.now());
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