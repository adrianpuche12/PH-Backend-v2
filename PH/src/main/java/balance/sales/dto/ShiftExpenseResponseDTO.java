package balance.sales.dto;

import balance.sales.model.ShiftExpense;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ShiftExpenseResponseDTO {

    private Long id;
    private Long shiftId;
    private String description;
    private BigDecimal amount;
    private String username;
    private LocalDateTime createdAt;

    public static ShiftExpenseResponseDTO from(ShiftExpense e) {
        ShiftExpenseResponseDTO dto = new ShiftExpenseResponseDTO();
        dto.id          = e.getId();
        dto.shiftId     = e.getShift() != null ? e.getShift().getId() : null;
        dto.description = e.getDescription();
        dto.amount      = e.getAmount();
        dto.username    = e.getUsername();
        dto.createdAt   = e.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getShiftId() { return shiftId; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getUsername() { return username; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
