package balance.catalog.dto;

import balance.catalog.model.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductResponseDTO {

    private Long id;
    private String name;
    private String sku;
    private String type;
    private BigDecimal price;
    private Integer minStock;
    private Boolean active;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String categoryPath; // ej: "Bebidas > Cervezas > Nacionales"
    private Long storeId;
    private String storeName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponseDTO from(Product product) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.id          = product.getId();
        dto.name        = product.getName();
        dto.sku         = product.getSku();
        dto.type        = product.getType();
        dto.price       = product.getPrice();
        dto.minStock    = product.getMinStock();
        dto.active      = product.getActive();
        dto.description = product.getDescription();
        dto.createdAt   = product.getCreatedAt();
        dto.updatedAt   = product.getUpdatedAt();

        if (product.getCategory() != null) {
            dto.categoryId   = product.getCategory().getId();
            dto.categoryName = product.getCategory().getName();
            dto.categoryPath = buildCategoryPath(product.getCategory());
        }
        if (product.getStore() != null) {
            dto.storeId   = product.getStore().getId();
            dto.storeName = product.getStore().getName();
        }
        return dto;
    }

    // Construye el path jerárquico: "Bebidas > Cervezas > Nacionales"
    private static String buildCategoryPath(balance.catalog.model.Category category) {
        if (category == null) return null;
        if (category.getParent() == null) return category.getName();
        return buildCategoryPath(category.getParent()) + " > " + category.getName();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public String getType() { return type; }
    public BigDecimal getPrice() { return price; }
    public Integer getMinStock() { return minStock; }
    public Boolean getActive() { return active; }
    public String getDescription() { return description; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getCategoryPath() { return categoryPath; }
    public Long getStoreId() { return storeId; }
    public String getStoreName() { return storeName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
