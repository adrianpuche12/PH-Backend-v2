package balance.sales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class SaleRequestDTO {

    @NotBlank(message = "El usuario es obligatorio")
    private String username;

    @NotEmpty(message = "La venta debe tener al menos un producto")
    private List<SaleItemRequestDTO> items;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public List<SaleItemRequestDTO> getItems() { return items; }
    public void setItems(List<SaleItemRequestDTO> items) { this.items = items; }
}
