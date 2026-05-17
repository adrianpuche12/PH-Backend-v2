package balance.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Vista resumida de un local para el dashboard del admin. */
public class StoreDashboardDTO {

    private Long    storeId;
    private String  storeName;

    // Turno activo (null si no hay turno abierto)
    private boolean hasActiveShift;
    private String  shiftCode;
    private String  shiftUsername;
    private LocalDateTime shiftOpenedAt;
    private long    shiftSalesCount;
    private BigDecimal shiftSalesTotal;

    // Inventario
    private long       totalProducts;
    private long       lowStockCount;
    private BigDecimal estimatedValue;

    public StoreDashboardDTO() {}

    // ── Getters y setters ─────────────────────────────────────────────────────

    public Long getStoreId()                  { return storeId; }
    public void setStoreId(Long v)            { this.storeId = v; }

    public String getStoreName()              { return storeName; }
    public void setStoreName(String v)        { this.storeName = v; }

    public boolean isHasActiveShift()         { return hasActiveShift; }
    public void setHasActiveShift(boolean v)  { this.hasActiveShift = v; }

    public String getShiftCode()              { return shiftCode; }
    public void setShiftCode(String v)        { this.shiftCode = v; }

    public String getShiftUsername()          { return shiftUsername; }
    public void setShiftUsername(String v)    { this.shiftUsername = v; }

    public LocalDateTime getShiftOpenedAt()   { return shiftOpenedAt; }
    public void setShiftOpenedAt(LocalDateTime v) { this.shiftOpenedAt = v; }

    public long getShiftSalesCount()          { return shiftSalesCount; }
    public void setShiftSalesCount(long v)    { this.shiftSalesCount = v; }

    public BigDecimal getShiftSalesTotal()    { return shiftSalesTotal; }
    public void setShiftSalesTotal(BigDecimal v) { this.shiftSalesTotal = v; }

    public long getTotalProducts()            { return totalProducts; }
    public void setTotalProducts(long v)      { this.totalProducts = v; }

    public long getLowStockCount()            { return lowStockCount; }
    public void setLowStockCount(long v)      { this.lowStockCount = v; }

    public BigDecimal getEstimatedValue()     { return estimatedValue; }
    public void setEstimatedValue(BigDecimal v) { this.estimatedValue = v; }
}
