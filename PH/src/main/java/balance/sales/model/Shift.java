package balance.sales.model;

import balance.model.Store;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shifts")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // T-2026-0514-DAN

    @NotBlank
    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String status = "OPEN"; // OPEN | CLOSED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    // Totales de efectivo y tarjeta — calculados al cerrar el turno
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCashSales = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCardSales = BigDecimal.ZERO;

    // Depósito bancario
    @Column(nullable = false)
    private Boolean deposited = false;

    @Column(name = "deposit_id")
    private Long depositId;

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }

    public LocalDateTime getOpenedAt() { return openedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public BigDecimal getTotalCashSales() { return totalCashSales; }
    public void setTotalCashSales(BigDecimal totalCashSales) { this.totalCashSales = totalCashSales; }

    public BigDecimal getTotalCardSales() { return totalCardSales; }
    public void setTotalCardSales(BigDecimal totalCardSales) { this.totalCardSales = totalCardSales; }

    public Boolean getDeposited() { return deposited; }
    public void setDeposited(Boolean deposited) { this.deposited = deposited; }

    public Long getDepositId() { return depositId; }
    public void setDepositId(Long depositId) { this.depositId = depositId; }
}
