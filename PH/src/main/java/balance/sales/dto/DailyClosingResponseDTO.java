package balance.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyClosingResponseDTO {
    private Long shiftId;
    private String shiftCode;
    private LocalDate date;
    private Long storeId;
    private String storeName;
    private long salesConfirmed;
    private BigDecimal totalAmount;
    private BigDecimal openingCashAmount;
    private BigDecimal totalCashSales;
    private BigDecimal totalShiftExpenses;
    private BigDecimal expectedCashAmount;   // openingCashAmount + totalCashSales - totalShiftExpenses
    private BigDecimal totalCardSales;
    private BigDecimal totalCardSurcharge;
    private BigDecimal declaredCashAmount;
    private BigDecimal cashDifference;
    private Long closingDepositId;
    private String message;

    public DailyClosingResponseDTO(Long shiftId, String shiftCode, LocalDate date,
                                   Long storeId, String storeName, long salesConfirmed,
                                   BigDecimal totalAmount, BigDecimal openingCashAmount,
                                   BigDecimal totalCashSales, BigDecimal totalShiftExpenses,
                                   BigDecimal totalCardSales, BigDecimal totalCardSurcharge,
                                   BigDecimal declaredCashAmount,
                                   BigDecimal cashDifference, Long closingDepositId) {
        this.shiftId             = shiftId;
        this.shiftCode           = shiftCode;
        this.date                = date;
        this.storeId             = storeId;
        this.storeName           = storeName;
        this.salesConfirmed      = salesConfirmed;
        this.totalAmount         = totalAmount;
        this.openingCashAmount   = openingCashAmount;
        this.totalCashSales      = totalCashSales;
        this.totalShiftExpenses  = totalShiftExpenses;
        this.expectedCashAmount  = openingCashAmount.add(totalCashSales).subtract(totalShiftExpenses);
        this.totalCardSales      = totalCardSales;
        this.totalCardSurcharge  = totalCardSurcharge;
        this.declaredCashAmount  = declaredCashAmount;
        this.cashDifference      = cashDifference;
        this.closingDepositId    = closingDepositId;
        this.message             = "Cierre de turno completado. " + salesConfirmed + " ventas confirmadas.";
    }

    public Long getShiftId() { return shiftId; }
    public String getShiftCode() { return shiftCode; }
    public LocalDate getDate() { return date; }
    public Long getStoreId() { return storeId; }
    public String getStoreName() { return storeName; }
    public long getSalesConfirmed() { return salesConfirmed; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getOpeningCashAmount() { return openingCashAmount; }
    public BigDecimal getTotalCashSales() { return totalCashSales; }
    public BigDecimal getTotalShiftExpenses() { return totalShiftExpenses; }
    public BigDecimal getExpectedCashAmount() { return expectedCashAmount; }
    public BigDecimal getTotalCardSales() { return totalCardSales; }
    public BigDecimal getTotalCardSurcharge() { return totalCardSurcharge; }
    public BigDecimal getDeclaredCashAmount() { return declaredCashAmount; }
    public BigDecimal getCashDifference() { return cashDifference; }
    public Long getClosingDepositId() { return closingDepositId; }
    public String getMessage() { return message; }
}
