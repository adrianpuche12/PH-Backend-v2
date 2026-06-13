package balance.sales.repository;

import balance.model.Store;
import balance.repository.StoreRepository;
import balance.sales.model.Shift;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(locations = "classpath:application-test.properties")
class ShiftRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired ShiftRepository shiftRepository;
    @Autowired StoreRepository storeRepository;

    private Store store;

    @BeforeEach
    void setup() {
        shiftRepository.deleteAll();
        storeRepository.deleteAll();

        store = new Store();
        store.setName("Danli Test");
        store = storeRepository.save(store);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Shift saveShift(String status, String code) {
        return saveShift(status, code, "cajero01");
    }

    private Shift saveShift(String status, String code, String username) {
        Shift shift = new Shift();
        shift.setStore(store);
        shift.setUsername(username);
        shift.setStatus(status);
        shift.setCode(code);
        return shiftRepository.save(shift);
    }

    // ── existsByStoreIdAndUsernameAndStatus ───────────────────────────────────

    @Test
    void existsByStoreIdAndUsernameAndStatus_returnsTrueWhenUserHasOpenShift() {
        saveShift("OPEN", "T-20260514-0900-DAN", "cajero01");

        boolean exists = shiftRepository.existsByStoreIdAndUsernameAndStatus(store.getId(), "cajero01", "OPEN");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByStoreIdAndUsernameAndStatus_returnsFalseWhenUserShiftIsClosed() {
        saveShift("CLOSED", "T-20260514-0900-DAN", "cajero01");

        boolean exists = shiftRepository.existsByStoreIdAndUsernameAndStatus(store.getId(), "cajero01", "OPEN");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByStoreIdAndUsernameAndStatus_returnsFalseWhenStoreHasNoShifts() {
        boolean exists = shiftRepository.existsByStoreIdAndUsernameAndStatus(store.getId(), "cajero01", "OPEN");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByStoreIdAndUsernameAndStatus_returnsFalseForDifferentUser_evenWithOpenShiftInSameStore() {
        // cajero01 tiene turno abierto, pero cajero02 no — debe poder abrir el suyo
        saveShift("OPEN", "T-20260514-0900-DAN", "cajero01");

        boolean exists = shiftRepository.existsByStoreIdAndUsernameAndStatus(store.getId(), "cajero02", "OPEN");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByStoreIdAndUsernameAndStatus_returnsFalseForDifferentStore() {
        Store otraStore = new Store();
        otraStore.setName("El Paraíso");
        otraStore = storeRepository.save(otraStore);

        Shift shift = new Shift();
        shift.setStore(otraStore);
        shift.setUsername("cajero01");
        shift.setStatus("OPEN");
        shift.setCode("T-20260514-0900-ELP");
        shiftRepository.save(shift);

        // El local principal no tiene turno abierto para cajero01
        boolean exists = shiftRepository.existsByStoreIdAndUsernameAndStatus(store.getId(), "cajero01", "OPEN");

        assertThat(exists).isFalse();
    }

    // ── findByStoreIdAndUsernameAndStatus ─────────────────────────────────────

    @Test
    void findByStoreIdAndUsernameAndStatus_returnsOpenShiftOfThatUser() {
        saveShift("OPEN", "T-20260514-0900-DAN", "cajero01");

        Optional<Shift> result = shiftRepository.findByStoreIdAndUsernameAndStatus(store.getId(), "cajero01", "OPEN");

        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("T-20260514-0900-DAN");
    }

    @Test
    void findByStoreIdAndUsernameAndStatus_returnsEmptyWhenShiftIsClosed() {
        saveShift("CLOSED", "T-20260514-0900-DAN", "cajero01");

        Optional<Shift> result = shiftRepository.findByStoreIdAndUsernameAndStatus(store.getId(), "cajero01", "OPEN");

        assertThat(result).isEmpty();
    }

    @Test
    void findByStoreIdAndUsernameAndStatus_doesNotReturnAnotherUsersOpenShift() {
        // cajero01 abre turno; cajero02 consulta el suyo y no debe ver el de cajero01
        saveShift("OPEN", "T-20260514-0900-DAN", "cajero01");

        Optional<Shift> result = shiftRepository.findByStoreIdAndUsernameAndStatus(store.getId(), "cajero02", "OPEN");

        assertThat(result).isEmpty();
    }

    // ── findByStoreIdAndStatusOrderByOpenedAtDesc (multi-turno por local) ─────

    @Test
    void findByStoreIdAndStatusOrderByOpenedAtDesc_returnsAllOpenShiftsForMultipleCashiers() throws InterruptedException {
        saveShift("OPEN", "T-20260514-0900-DAN", "cajero01");
        Thread.sleep(10);
        saveShift("OPEN", "T-20260514-0910-DAN", "cajero02");

        List<Shift> result = shiftRepository.findByStoreIdAndStatusOrderByOpenedAtDesc(store.getId(), "OPEN");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Shift::getUsername).containsExactlyInAnyOrder("cajero01", "cajero02");
    }

    @Test
    void findByStoreIdAndStatusOrderByOpenedAtDesc_returnsEmptyWhenNoOpenShifts() {
        saveShift("CLOSED", "T-20260514-0900-DAN", "cajero01");

        List<Shift> result = shiftRepository.findByStoreIdAndStatusOrderByOpenedAtDesc(store.getId(), "OPEN");

        assertThat(result).isEmpty();
    }

    // ── findByStoreIdOrderByOpenedAtDesc ──────────────────────────────────────

    @Test
    void findByStoreIdOrderByOpenedAtDesc_returnsMostRecentFirst() throws InterruptedException {
        Shift first  = saveShift("CLOSED", "T-20260514-0800-DAN");
        Thread.sleep(10); // asegurar diferencia de timestamp
        Shift second = saveShift("CLOSED", "T-20260514-0900-DAN");
        Thread.sleep(10);
        Shift third  = saveShift("OPEN",   "T-20260514-1000-DAN");

        List<Shift> result = shiftRepository.findByStoreIdOrderByOpenedAtDesc(store.getId());

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getCode()).isEqualTo("T-20260514-1000-DAN"); // más reciente primero
        assertThat(result.get(2).getCode()).isEqualTo("T-20260514-0800-DAN"); // más antiguo al final
    }

    @Test
    void findByStoreIdOrderByOpenedAtDesc_returnsEmptyWhenNoShifts() {
        List<Shift> result = shiftRepository.findByStoreIdOrderByOpenedAtDesc(store.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findByStoreIdOrderByOpenedAtDesc_onlyReturnsShiftsForRequestedStore() {
        saveShift("OPEN", "T-20260514-0900-DAN");

        Store otraStore = new Store();
        otraStore.setName("El Paraíso");
        otraStore = storeRepository.save(otraStore);
        Shift shiftOtra = new Shift();
        shiftOtra.setStore(otraStore);
        shiftOtra.setUsername("cajero02");
        shiftOtra.setStatus("OPEN");
        shiftOtra.setCode("T-20260514-0900-ELP");
        shiftRepository.save(shiftOtra);

        List<Shift> result = shiftRepository.findByStoreIdOrderByOpenedAtDesc(store.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("T-20260514-0900-DAN");
    }
}
