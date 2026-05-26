package balance.inventory.dto;

import java.math.BigDecimal;

public class StockSummaryDTO {

    private long totalProducts;
    private long activeProducts;
    private long lowStockCount;
    private long categoryCount;
    private BigDecimal estimatedValue;

    public StockSummaryDTO(long totalProducts, long activeProducts,
                           long lowStockCount, long categoryCount,
                           BigDecimal estimatedValue) {
        this.totalProducts  = totalProducts;
        this.activeProducts = activeProducts;
        this.lowStockCount  = lowStockCount;
        this.categoryCount  = categoryCount;
        this.estimatedValue = estimatedValue;
    }

    public long getTotalProducts()  { return totalProducts; }
    public long getActiveProducts() { return activeProducts; }
    public long getLowStockCount()  { return lowStockCount; }
    public long getCategoryCount()  { return categoryCount; }
    public BigDecimal getEstimatedValue() { return estimatedValue; }
}
