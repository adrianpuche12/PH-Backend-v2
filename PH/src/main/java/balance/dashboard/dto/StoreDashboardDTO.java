package balance.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Vista resumida de un local para el dashboard del admin. */
public class StoreDashboardDTO {

    private Long    storeId;
    private String  storeName;

    // Turnos abiertos (puede haber 0, 1 o varios — uno por cajero)
    private boolean hasActiveShift;
    private List<ActiveShiftDTO> activeShifts;
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

    public List<ActiveShiftDTO> getActiveShifts()       { return activeShifts; }
    public void setActiveShifts(List<ActiveShiftDTO> v) { this.activeShifts = v; }

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

    /** Resumen de un turno abierto individual (un cajero). */
    public static class ActiveShiftDTO {
        private String code;
        private String username;
        private LocalDateTime openedAt;
        private long salesCount;
        private BigDecimal salesTotal;

        public ActiveShiftDTO() {}

        public ActiveShiftDTO(String code, String username, LocalDateTime openedAt,
                               long salesCount, BigDecimal salesTotal) {
            this.code = code;
            this.username = username;
            this.openedAt = openedAt;
            this.salesCount = salesCount;
            this.salesTotal = salesTotal;
        }

        public String getCode()                  { return code; }
        public void setCode(String v)            { this.code = v; }

        public String getUsername()              { return username; }
        public void setUsername(String v)        { this.username = v; }

        public LocalDateTime getOpenedAt()       { return openedAt; }
        public void setOpenedAt(LocalDateTime v) { this.openedAt = v; }

        public long getSalesCount()              { return salesCount; }
        public void setSalesCount(long v)        { this.salesCount = v; }

        public BigDecimal getSalesTotal()        { return salesTotal; }
        public void setSalesTotal(BigDecimal v)  { this.salesTotal = v; }
    }
}
