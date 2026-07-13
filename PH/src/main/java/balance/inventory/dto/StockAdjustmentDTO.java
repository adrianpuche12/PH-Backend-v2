package balance.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class StockAdjustmentDTO {

    @NotNull(message = "El producto es obligatorio")
    private Long productId;

    // ENTRADA | SALIDA | AJUSTE
    @NotBlank(message = "El tipo es obligatorio")
    private String type;

    @NotNull
    @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a 0")
    private BigDecimal quantity;

    private String reason;
    private String notes;
    private String username;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
