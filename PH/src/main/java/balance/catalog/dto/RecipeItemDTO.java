package balance.catalog.dto;

import balance.catalog.model.ProductRecipeItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class RecipeItemDTO {

    // --- Request fields ---
    @NotNull(message = "El ingredienteId es obligatorio")
    private Long ingredientId;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    private BigDecimal quantity;

    // --- Response-only fields ---
    private Long id;
    private String ingredientName;
    private String ingredientSku;

    public RecipeItemDTO() {}

    public static RecipeItemDTO from(ProductRecipeItem item) {
        RecipeItemDTO dto = new RecipeItemDTO();
        dto.id              = item.getId();
        dto.ingredientId    = item.getIngredient().getId();
        dto.ingredientName  = item.getIngredient().getName();
        dto.ingredientSku   = item.getIngredient().getSku();
        dto.quantity        = item.getQuantity();
        return dto;
    }

    public Long getId()                    { return id; }
    public Long getIngredientId()          { return ingredientId; }
    public void setIngredientId(Long v)    { this.ingredientId = v; }
    public BigDecimal getQuantity()        { return quantity; }
    public void setQuantity(BigDecimal v)  { this.quantity = v; }
    public String getIngredientName()      { return ingredientName; }
    public String getIngredientSku()       { return ingredientSku; }
}
