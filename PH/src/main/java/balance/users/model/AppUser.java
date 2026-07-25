package balance.users.model;

import balance.model.Store;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String keycloakId;

    @NotBlank
    @Column(nullable = false)
    private String fullName;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String username;

    @Column
    private String email;

    /** Rol de negocio: ADMIN, ENCARGADO, CONTADOR, SOCIO */
    @Column
    private String role;

    /** true hasta que el usuario cambie su contraseña temporal en el primer login */
    @Column(nullable = false)
    private boolean firstLogin = false;

    /** Local principal donde trabaja el empleado (puede ser null para CONTADOR/SOCIO) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    /**
     * Locales cuya data puede ver este usuario.
     * Lista vacía = acceso a todos los locales (compatibilidad con usuarios existentes).
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_store_access",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "store_id")
    )
    private List<Store> accessibleStores = new ArrayList<>();

    /**
     * Secciones del sistema que el usuario puede ver.
     * Lista vacía = acceso a todas las secciones (compatibilidad con usuarios existentes).
     * Valores: DASHBOARD, POS, SALES_HISTORY, INVENTORY, TRANSACTIONS, SALARY_PAYMENTS, SUPPLIER_PAYMENTS, CATALOG
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission")
    private List<String> permissions = new ArrayList<>();

    @Column(nullable = false)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Long getId()                              { return id; }

    public String getKeycloakId()                   { return keycloakId; }
    public void setKeycloakId(String v)             { this.keycloakId = v; }

    public String getFullName()                     { return fullName; }
    public void setFullName(String v)               { this.fullName = v; }

    public String getUsername()                     { return username; }
    public void setUsername(String v)               { this.username = v; }

    public String getEmail()                        { return email; }
    public void setEmail(String v)                  { this.email = v; }

    public String getRole()                         { return role; }
    public void setRole(String v)                   { this.role = v; }

    public boolean isFirstLogin()                   { return firstLogin; }
    public void setFirstLogin(boolean v)            { this.firstLogin = v; }

    public Store getStore()                         { return store; }
    public void setStore(Store v)                   { this.store = v; }

    public List<Store> getAccessibleStores()        { return accessibleStores; }
    public void setAccessibleStores(List<Store> v)  { this.accessibleStores = v; }

    public List<String> getPermissions()            { return permissions; }
    public void setPermissions(List<String> v)      { this.permissions = v; }

    public String getStatus()                       { return status; }
    public void setStatus(String v)                 { this.status = v; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
}
