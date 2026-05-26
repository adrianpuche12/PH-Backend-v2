package balance.catalog.dto;

import balance.model.Store;

import java.time.LocalDateTime;

public class StoreResponseDTO {

    private Long id;
    private String name;
    private String address;
    private String phone;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StoreResponseDTO from(Store store) {
        StoreResponseDTO dto = new StoreResponseDTO();
        dto.id        = store.getId();
        dto.name      = store.getName();
        dto.address   = store.getAddress();
        dto.phone     = store.getPhone();
        dto.active    = store.getActive();
        dto.createdAt = store.getCreatedAt();
        dto.updatedAt = store.getUpdatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
