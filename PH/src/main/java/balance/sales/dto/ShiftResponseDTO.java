package balance.sales.dto;

import balance.sales.model.Shift;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ShiftResponseDTO {
    private Long id;
    private String code;
    private String username;
    private String status;
    private Long storeId;
    private String storeName;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private BigDecimal openingCashAmount;
    private BigDecimal totalCashSales;
    private BigDecimal totalCardSales;
    private BigDecimal totalShiftExpenses;
    private BigDecimal declaredCashAmount;
    private BigDecimal cashDifference;

    public static ShiftResponseDTO from(Shift s) {
        ShiftResponseDTO dto = new ShiftResponseDTO();
        dto.id                 = s.getId();
        dto.code               = s.getCode();
        dto.username           = s.getUsername();
        dto.status             = s.getStatus();
        dto.openedAt           = s.getOpenedAt();
        dto.closedAt           = s.getClosedAt();
        dto.openingCashAmount   = s.getOpeningCashAmount();
        dto.totalCashSales      = s.getTotalCashSales();
        dto.totalCardSales      = s.getTotalCardSales();
        dto.totalShiftExpenses  = s.getTotalShiftExpenses();
        dto.declaredCashAmount  = s.getDeclaredCashAmount();
        dto.cashDifference      = s.getCashDifference();
        if (s.getStore() != null) {
            dto.storeId   = s.getStore().getId();
            dto.storeName = s.getStore().getName();
        }
        return dto;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getUsername() { return username; }
    public String getStatus() { return status; }
    public Long getStoreId() { return storeId; }
    public String getStoreName() { return storeName; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public BigDecimal getOpeningCashAmount() { return openingCashAmount; }
    public BigDecimal getTotalCashSales() { return totalCashSales; }
    public BigDecimal getTotalShiftExpenses() { return totalShiftExpenses; }
    public BigDecimal getTotalCardSales() { return totalCardSales; }
    public BigDecimal getDeclaredCashAmount() { return declaredCashAmount; }
    public BigDecimal getCashDifference() { return cashDifference; }
}
