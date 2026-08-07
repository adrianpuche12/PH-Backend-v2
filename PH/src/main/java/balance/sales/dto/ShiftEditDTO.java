package balance.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ShiftEditDTO {

    private String username;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private BigDecimal openingCashAmount;
    private BigDecimal totalCashSales;
    private BigDecimal totalCardSales;
    private BigDecimal totalShiftExpenses;
    private BigDecimal declaredCashAmount;
    private String notes;

    public String getUsername()                { return username; }
    public void setUsername(String v)          { this.username = v; }

    public LocalDateTime getOpenedAt()         { return openedAt; }
    public void setOpenedAt(LocalDateTime v)   { this.openedAt = v; }

    public LocalDateTime getClosedAt()         { return closedAt; }
    public void setClosedAt(LocalDateTime v)   { this.closedAt = v; }

    public BigDecimal getOpeningCashAmount()          { return openingCashAmount; }
    public void setOpeningCashAmount(BigDecimal v)    { this.openingCashAmount = v; }

    public BigDecimal getTotalCashSales()             { return totalCashSales; }
    public void setTotalCashSales(BigDecimal v)       { this.totalCashSales = v; }

    public BigDecimal getTotalCardSales()             { return totalCardSales; }
    public void setTotalCardSales(BigDecimal v)       { this.totalCardSales = v; }

    public BigDecimal getTotalShiftExpenses()         { return totalShiftExpenses; }
    public void setTotalShiftExpenses(BigDecimal v)   { this.totalShiftExpenses = v; }

    public BigDecimal getDeclaredCashAmount()         { return declaredCashAmount; }
    public void setDeclaredCashAmount(BigDecimal v)   { this.declaredCashAmount = v; }

    public String getNotes()                   { return notes; }
    public void setNotes(String v)             { this.notes = v; }
}
