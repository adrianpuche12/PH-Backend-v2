package balance.catalog.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "product_recipe_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "ingredient_id"}))
public class ProductRecipeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El producto manufacturado al que pertenece esta receta
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // La materia prima (producto SIMPLE en inventario)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Product ingredient;

    // Cantidad de materia prima por 1 unidad del manufacturado (soporta fracciones)
    @NotNull
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    public ProductRecipeItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Product getIngredient() { return ingredient; }
    public void setIngredient(Product ingredient) { this.ingredient = ingredient; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
