package balance.deposit.dto;

import balance.sales.model.Shift;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PendingShiftResponse {

    private Long id;
    private String code;
    private String storeName;
    private Long storeId;
    private String username;
    private LocalDateTime closedAt;
    private BigDecimal totalCashSales;
    private BigDecimal totalCardSales;
    private BigDecimal totalSales;

    public static PendingShiftResponse from(Shift s) {
        PendingShiftResponse r = new PendingShiftResponse();
        r.id             = s.getId();
        r.code           = s.getCode();
        r.storeName      = s.getStore().getName();
        r.storeId        = s.getStore().getId();
        r.username       = s.getUsername();
        r.closedAt       = s.getClosedAt();
        r.totalCashSales = s.getTotalCashSales();
        r.totalCardSales = s.getTotalCardSales();
        r.totalSales     = s.getTotalCashSales().add(s.getTotalCardSales());
        return r;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getStoreName() { return storeName; }
    public Long getStoreId() { return storeId; }
    public String getUsername() { return username; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public BigDecimal getTotalCashSales() { return totalCashSales; }
    public BigDecimal getTotalCardSales() { return totalCardSales; }
    public BigDecimal getTotalSales() { return totalSales; }
}
