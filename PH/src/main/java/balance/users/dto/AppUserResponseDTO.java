package balance.users.dto;

import balance.users.model.AppUser;

import java.time.LocalDateTime;
import java.util.List;

public class AppUserResponseDTO {

    private Long          id;
    private String        fullName;
    private String        username;
    private String        email;
    private String        role;
    private boolean       firstLogin;
    private String        status;
    private Long          storeId;
    private String        storeName;
    private List<Long>    storeIds;
    private List<String>  permissions;
    private LocalDateTime createdAt;

    public static AppUserResponseDTO from(AppUser u) {
        AppUserResponseDTO dto = new AppUserResponseDTO();
        dto.id          = u.getId();
        dto.fullName    = u.getFullName();
        dto.username    = u.getUsername();
        dto.email       = u.getEmail();
        dto.role        = u.getRole();
        dto.firstLogin  = u.isFirstLogin();
        dto.status      = u.getStatus();
        dto.permissions = u.getPermissions();
        dto.createdAt   = u.getCreatedAt();

        if (u.getStore() != null) {
            dto.storeId   = u.getStore().getId();
            dto.storeName = u.getStore().getName();
        }

        dto.storeIds = u.getAccessibleStores().stream()
                .map(s -> s.getId())
                .toList();

        return dto;
    }

    public Long getId()                  { return id; }
    public String getFullName()          { return fullName; }
    public String getUsername()          { return username; }
    public String getEmail()             { return email; }
    public String getRole()              { return role; }
    public boolean isFirstLogin()        { return firstLogin; }
    public String getStatus()            { return status; }
    public Long getStoreId()             { return storeId; }
    public String getStoreName()         { return storeName; }
    public List<Long> getStoreIds()      { return storeIds; }
    public List<String> getPermissions() { return permissions; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}
