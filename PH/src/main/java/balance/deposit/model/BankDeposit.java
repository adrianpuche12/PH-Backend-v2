package balance.deposit.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_deposits")
public class BankDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quién registró el depósito
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    // Fecha en que se realizó el depósito en el banco
    @Column(name = "deposit_date", nullable = false)
    private LocalDate depositDate;

    // IDs de los locales incluidos (texto separado por comas)
    @Column(name = "store_ids", nullable = false)
    private String storeIds;

    // IDs de los cierres incluidos (texto separado por comas)
    @Column(name = "shift_ids", nullable = false)
    private String shiftIds;

    // Suma del efectivo de todos los cierres seleccionados
    @Column(name = "expected_cash", nullable = false, precision = 12, scale = 2)
    private BigDecimal expectedCash;

    // Lo que el empleado declara que va a depositar
    @Column(name = "declared_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal declaredAmount;

    // declaredAmount - expectedCash (positivo = sobra, negativo = falta)
    @Column(name = "difference", nullable = false, precision = 12, scale = 2)
    private BigDecimal difference;

    // PENDING | CONFIRMED | DISCREPANCY
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    // Foto del comprobante bancario (opcional)
    @Column(name = "image_uri", length = 512)
    private String imageUri;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDate getDepositDate() { return depositDate; }
    public void setDepositDate(LocalDate depositDate) { this.depositDate = depositDate; }

    public String getStoreIds() { return storeIds; }
    public void setStoreIds(String storeIds) { this.storeIds = storeIds; }

    public String getShiftIds() { return shiftIds; }
    public void setShiftIds(String shiftIds) { this.shiftIds = shiftIds; }

    public BigDecimal getExpectedCash() { return expectedCash; }
    public void setExpectedCash(BigDecimal expectedCash) { this.expectedCash = expectedCash; }

    public BigDecimal getDeclaredAmount() { return declaredAmount; }
    public void setDeclaredAmount(BigDecimal declaredAmount) { this.declaredAmount = declaredAmount; }

    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
