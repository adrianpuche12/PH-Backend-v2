package balance.users.repository;

import balance.users.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    List<AppUser> findByStoreIdOrderByFullNameAsc(Long storeId);
    List<AppUser> findAllByOrderByFullNameAsc();
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByKeycloakId(String keycloakId);
    boolean existsByUsername(String username);
}
