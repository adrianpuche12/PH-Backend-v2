package balance.inventory.dto;

import balance.inventory.model.InventoryStock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockItemDTO {

    private Long stockId;
    private Long productId;
    private String productName;
    private String productSku;
    private String productType;
    private Boolean productActive;
    private BigDecimal price;
    private Integer quantity;
    private Integer minStock;
    private boolean lowStock;
    private String categoryName;
    private String categoryPath;
    private Long categoryId;
    private Long storeId;
    private String storeName;
    private LocalDateTime updatedAt;

    public static StockItemDTO from(InventoryStock stock) {
        StockItemDTO dto = new StockItemDTO();
        dto.stockId       = stock.getId();
        dto.quantity      = stock.getQuantity();
        dto.updatedAt     = stock.getUpdatedAt();

        var p = stock.getProduct();
        dto.productId     = p.getId();
        dto.productName   = p.getName();
        dto.productSku    = p.getSku();
        dto.productType   = p.getType();
        dto.productActive = p.getActive();
        dto.price         = p.getPrice();
        dto.minStock      = p.getMinStock();
        // lowStock solo aplica cuando hay un mÃ­nimo definido (minStock > 0)
        dto.lowStock      = p.getMinStock() > 0 && stock.getQuantity() <= p.getMinStock();

        if (p.getCategory() != null) {
            dto.categoryName = p.getCategory().getName();
            dto.categoryPath = buildPath(p.getCategory());
            dto.categoryId   = p.getCategory().getId();
        }
        if (stock.getStore() != null) {
            dto.storeId   = stock.getStore().getId();
            dto.storeName = stock.getStore().getName();
        }
        return dto;
    }

    private static String buildPath(balance.catalog.model.Category cat) {
        if (cat == null) return null;
        if (cat.getParent() == null) return cat.getName();
        return buildPath(cat.getParent()) + " > " + cat.getName();
    }

    public Long getStockId() { return stockId; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductSku() { return productSku; }
    public String getProductType() { return productType; }
    public Boolean getProductActive() { return productActive; }
    public BigDecimal getPrice() { return price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getMinStock() { return minStock; }
    public boolean isLowStock() { return lowStock; }
    public String getCategoryName() { return categoryName; }
    public String getCategoryPath() { return categoryPath; }
    public Long getCategoryId() { return categoryId; }
    public Long getStoreId() { return storeId; }
    public String getStoreName() { return storeName; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
