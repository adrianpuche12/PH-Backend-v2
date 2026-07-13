package balance.inventory.dto;

import balance.inventory.model.InventoryMovement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MovementDTO {

    private Long id;
    private String type;
    private BigDecimal quantity;
    private String reason;
    private String notes;
    private String username;
    private Long productId;
    private String productName;
    private Long storeId;
    private LocalDateTime createdAt;

    public static MovementDTO from(InventoryMovement m) {
        MovementDTO dto = new MovementDTO();
        dto.id          = m.getId();
        dto.type        = m.getType();
        dto.quantity    = m.getQuantity();
        dto.reason      = m.getReason();
        dto.notes       = m.getNotes();
        dto.username    = m.getUsername();
        dto.createdAt   = m.getCreatedAt();
        if (m.getProduct() != null) {
            dto.productId   = m.getProduct().getId();
            dto.productName = m.getProduct().getName();
        }
        if (m.getStore() != null) dto.storeId = m.getStore().getId();
        return dto;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public BigDecimal getQuantity() { return quantity; }
    public String getReason() { return reason; }
    public String getNotes() { return notes; }
    public String getUsername() { return username; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Long getStoreId() { return storeId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
