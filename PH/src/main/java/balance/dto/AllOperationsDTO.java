package balance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;


// ================================================================
// AGREGAR ESTE IMPORT AL INICIO DEL ARCHIVO
// ================================================================
import balance.model.GastoAdmin;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AllOperationsDTO {
    private Long id;
    private String type; // "CLOSING", "SUPPLIER", "SALARY"
    private BigDecimal amount;
    private LocalDate date;
    private LocalDate depositDate;
    private LocalDate paymentDate;
    private LocalDate salaryDate;

    private String description;
    private String username;
    private Long storeId;
    private String storeName;

    // Campos específicos de Closing Deposits
    private Integer closingsCount;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    // Campos específicos de Supplier Payments
    private String supplier;

    // Comprobante (URL pública en Cloudflare R2)
    private String imageUri;

    // Estado del depósito bancario (solo para CLOSING)
    private String depositStatus;
    private Long bankDepositId;

    // shiftId guardado en ClosingDeposit — usado internamente por enrichClosingDTOs
    private Long closingShiftId;

    // Datos del turno de ventas vinculado (solo para CLOSING generado por POS)
    private Long shiftId;
    private String shiftCode;
    private java.time.LocalDateTime shiftOpenedAt;
    private java.time.LocalDateTime shiftClosedAt;
    private Integer salesCount;
    private java.math.BigDecimal openingCashAmount;
    private java.math.BigDecimal totalCashSales;
    private java.math.BigDecimal totalCardSales;
    private java.math.BigDecimal totalShiftExpenses;
    private java.math.BigDecimal declaredCashAmount;
    private java.math.BigDecimal cashDifference;
    private String shiftNotes;

    // Constructores para cada tipo de operación
    public static AllOperationsDTO fromClosingDeposit(balance.model.ClosingDeposit deposit) {
        AllOperationsDTO dto = new AllOperationsDTO();
        dto.setId(deposit.getId());
        dto.setType("CLOSING");
        dto.setAmount(deposit.getAmount());
        dto.setDate(deposit.getDepositDate());
        dto.setDepositDate(deposit.getDepositDate());
        dto.setUsername(deposit.getUsername());
        if (deposit.getStore() != null) {
            dto.setStoreId(deposit.getStore().getId());
            dto.setStoreName(deposit.getStore().getName());
        }
        dto.setClosingsCount(deposit.getClosingsCount());
        dto.setPeriodStart(deposit.getPeriodStart());
        dto.setPeriodEnd(deposit.getPeriodEnd());
        dto.setDescription("Depósito de " + deposit.getClosingsCount() + " cierres");
        dto.setImageUri(deposit.getImageUri());
        dto.setDepositStatus(deposit.getDepositStatus());
        dto.setBankDepositId(deposit.getBankDepositId());
        dto.setClosingShiftId(deposit.getShiftId());   // link al turno que generó este cierre
        return dto;
    }

    public static AllOperationsDTO fromSupplierPayment(balance.model.SupplierPayment payment) {
        AllOperationsDTO dto = new AllOperationsDTO();
        dto.setId(payment.getId());
        dto.setType("SUPPLIER");
        dto.setAmount(payment.getAmount());
        dto.setDate(payment.getPaymentDate());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setUsername(payment.getUsername());
        if (payment.getStore() != null) {
            dto.setStoreId(payment.getStore().getId());
            dto.setStoreName(payment.getStore().getName());
        }
        dto.setSupplier(payment.getSupplier());

        if (payment.getDescription() != null && !payment.getDescription().isEmpty()) {
            dto.setDescription(payment.getDescription());
        } else {
            dto.setDescription("Pago a proveedor: " + payment.getSupplier());
        }
        dto.setImageUri(payment.getImageUri());
        return dto;
    }

    public static AllOperationsDTO fromSalaryPayment(balance.model.SalaryPayment payment) {
        AllOperationsDTO dto = new AllOperationsDTO();
        dto.setId(payment.getId());
        dto.setType("SALARY");
        dto.setAmount(payment.getAmount());
        dto.setDate(payment.getSalaryDate());
        dto.setSalaryDate(payment.getSalaryDate());
        dto.setDescription(payment.getDescription());
        dto.setUsername(payment.getUsername());
        if (payment.getStore() != null) {
            dto.setStoreId(payment.getStore().getId());
            dto.setStoreName(payment.getStore().getName());
        }
        dto.setImageUri(payment.getImageUri());
        return dto;
    }

    // Getters y Setters
    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getClosingsCount() {
        return closingsCount;
    }

    public void setClosingsCount(Integer closingsCount) {
        this.closingsCount = closingsCount;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public LocalDate getDepositDate() {
        return depositDate;
    }

    public void setDepositDate(LocalDate depositDate) {
        this.depositDate = depositDate;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public LocalDate getSalaryDate() {
        return salaryDate;
    }

    public void setSalaryDate(LocalDate salaryDate) {
        this.salaryDate = salaryDate;
    }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public String getDepositStatus() { return depositStatus; }
    public void setDepositStatus(String depositStatus) { this.depositStatus = depositStatus; }

    public Long getBankDepositId() { return bankDepositId; }
    public void setBankDepositId(Long bankDepositId) { this.bankDepositId = bankDepositId; }

    public Long getClosingShiftId() { return closingShiftId; }
    public void setClosingShiftId(Long closingShiftId) { this.closingShiftId = closingShiftId; }

    // Datos del turno vinculado
    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }

    public String getShiftCode() { return shiftCode; }
    public void setShiftCode(String shiftCode) { this.shiftCode = shiftCode; }

    public java.time.LocalDateTime getShiftOpenedAt() { return shiftOpenedAt; }
    public void setShiftOpenedAt(java.time.LocalDateTime shiftOpenedAt) { this.shiftOpenedAt = shiftOpenedAt; }

    public java.time.LocalDateTime getShiftClosedAt() { return shiftClosedAt; }
    public void setShiftClosedAt(java.time.LocalDateTime shiftClosedAt) { this.shiftClosedAt = shiftClosedAt; }

    public Integer getSalesCount() { return salesCount; }
    public void setSalesCount(Integer salesCount) { this.salesCount = salesCount; }

    public java.math.BigDecimal getOpeningCashAmount() { return openingCashAmount; }
    public void setOpeningCashAmount(java.math.BigDecimal openingCashAmount) { this.openingCashAmount = openingCashAmount; }

    public java.math.BigDecimal getTotalCashSales() { return totalCashSales; }
    public void setTotalCashSales(java.math.BigDecimal totalCashSales) { this.totalCashSales = totalCashSales; }

    public java.math.BigDecimal getTotalCardSales() { return totalCardSales; }
    public void setTotalCardSales(java.math.BigDecimal totalCardSales) { this.totalCardSales = totalCardSales; }

    public java.math.BigDecimal getTotalShiftExpenses() { return totalShiftExpenses; }
    public void setTotalShiftExpenses(java.math.BigDecimal totalShiftExpenses) { this.totalShiftExpenses = totalShiftExpenses; }

    public java.math.BigDecimal getDeclaredCashAmount() { return declaredCashAmount; }
    public void setDeclaredCashAmount(java.math.BigDecimal declaredCashAmount) { this.declaredCashAmount = declaredCashAmount; }

    public java.math.BigDecimal getCashDifference() { return cashDifference; }
    public void setCashDifference(java.math.BigDecimal cashDifference) { this.cashDifference = cashDifference; }

    public String getShiftNotes() { return shiftNotes; }
    public void setShiftNotes(String shiftNotes) { this.shiftNotes = shiftNotes; }

    /** Enriquece el DTO con los datos del turno vinculado. Solo aplica a tipo CLOSING. */
    public void enrichWithShift(balance.sales.model.Shift shift, int salesCount) {
        this.shiftId             = shift.getId();
        this.shiftCode           = shift.getCode();
        this.shiftOpenedAt       = shift.getOpenedAt();
        this.shiftClosedAt       = shift.getClosedAt();
        this.salesCount          = salesCount;
        this.openingCashAmount   = shift.getOpeningCashAmount();
        this.totalCashSales      = shift.getTotalCashSales();
        this.totalCardSales      = shift.getTotalCardSales();
        this.totalShiftExpenses  = shift.getTotalShiftExpenses();
        this.declaredCashAmount  = shift.getDeclaredCashAmount();
        this.cashDifference      = shift.getCashDifference();
        this.shiftNotes          = shift.getNotes();
    }


    // ================================================================
// AGREGAR ESTE MÉTODO CONSTRUCTOR ESTÁTICO A LA CLASE AllOperationsDTO
// ================================================================

    public static AllOperationsDTO fromGastoAdmin(balance.model.GastoAdmin gastoAdmin) {
        AllOperationsDTO dto = new AllOperationsDTO();
        dto.setId(gastoAdmin.getId());
        dto.setType("GASTO_ADMIN");
        dto.setAmount(gastoAdmin.getMonto());
        dto.setDate(gastoAdmin.getFecha());
        dto.setUsername(gastoAdmin.getUsername());
        
        
        // No tiene store específico porque se divide entre locales
        dto.setStoreId(null);
        dto.setStoreName("Administrativo (Dividido)");
        
        // Descripción con información de división
        dto.setDescription(String.format("%s (Danli: %d%%, El Paraíso: %d%%)", 
            gastoAdmin.getDescripcion(),
            gastoAdmin.getPorcentajeDanli(),
            gastoAdmin.getPorcentajeParaiso()));
        
        return dto;
    }
}