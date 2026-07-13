package balance.inventory.model;

import balance.catalog.model.Product;
import balance.model.Store;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_stock",
       uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "store_id"}))
public class InventoryStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Long getId() { return id; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
