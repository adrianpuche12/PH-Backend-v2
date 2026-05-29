package balance.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public class StoreRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    private String address;

    private String phone;

    /** Si se informa, copia la estructura de categorías y productos del local indicado. */
    private Long sourceStoreId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getSourceStoreId() { return sourceStoreId; }
    public void setSourceStoreId(Long sourceStoreId) { this.sourceStoreId = sourceStoreId; }
}
