package balance.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

public class AppUserRequestDTO {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String fullName;

    @NotBlank(message = "El username es obligatorio")
    private String username;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene formato válido")
    private String email;

    /** ADMIN, ENCARGADO, CONTADOR, SOCIO */
    @NotBlank(message = "El rol es obligatorio")
    private String role;

    /** Local principal (opcional, para empleados con local asignado) */
    private Long storeId;

    /** Locales cuya data puede ver. Vacío = todos los locales. */
    private List<Long> storeIds = new ArrayList<>();

    /** Secciones habilitadas. Vacío = todas las secciones. */
    private List<String> permissions = new ArrayList<>();

    public String getFullName()              { return fullName; }
    public void setFullName(String v)        { this.fullName = v; }

    public String getUsername()              { return username; }
    public void setUsername(String v)        { this.username = v; }

    public String getEmail()                 { return email; }
    public void setEmail(String v)           { this.email = v; }

    public String getRole()                  { return role; }
    public void setRole(String v)            { this.role = v; }

    public Long getStoreId()                 { return storeId; }
    public void setStoreId(Long v)           { this.storeId = v; }

    public List<Long> getStoreIds()          { return storeIds; }
    public void setStoreIds(List<Long> v)    { this.storeIds = v != null ? v : new ArrayList<>(); }

    public List<String> getPermissions()     { return permissions; }
    public void setPermissions(List<String> v) { this.permissions = v != null ? v : new ArrayList<>(); }
}
