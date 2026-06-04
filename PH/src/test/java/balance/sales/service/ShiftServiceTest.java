package balance.sales.service;

import balance.model.Store;
import balance.repository.StoreRepository;
import balance.sales.dto.ShiftResponseDTO;
import balance.sales.model.Shift;
import balance.sales.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @InjectMocks private ShiftService shiftService;

    @Mock private ShiftRepository shiftRepository;
    @Mock private StoreRepository storeRepository;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Store buildStore(Long id, String name) {
        Store s = new Store();
        s.setId(id);
        s.setName(name);
        return s;
    }

    private Shift buildShift(Long id, Store store, String status) {
        Shift shift = new Shift();
        shift.setStore(store);
        shift.setStatus(status);
        shift.setCode("T-20260514-0900-DAN");
        shift.setUsername("cajero01");
        return shift;
    }

    // ── openShift — formato del código ────────────────────────────────────────

    @Test
    void openShift_codeMatchesExpectedPattern() {
        // Formato: T-YYYYMMDD-HHmmss-DAN (máx 3 letras del nombre del local, incluye segundos)
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(shiftRepository.existsByStoreIdAndStatus(1L, "OPEN")).thenReturn(false);
        when(shiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShiftResponseDTO result = shiftService.openShift(1L, "cajero01", null);

        assertThat(result.getCode()).matches("T-\\d{8}-\\d{6}-[A-Z]{1,3}");
    }

    @Test
    void openShift_codeEndsWithFirstThreeLettersOfStoreName() {
        // "Danli" → solo letras → "Danli" → uppercase → "DANLI" → substring(0,3) → "DAN"
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(shiftRepository.existsByStoreIdAndStatus(1L, "OPEN")).thenReturn(false);
        when(shiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShiftResponseDTO result = shiftService.openShift(1L, "cajero01", null);

        assertThat(result.getCode()).endsWith("-DAN");
    }

    @Test
    void openShift_codeStripsNonLettersFromStoreName() {
        // "El Paraiso" → solo letras → "ElParaiso" → uppercase → "ELPARAISO" → substring(0,3) → "ELP"
        when(storeRepository.findById(2L)).thenReturn(Optional.of(buildStore(2L, "El Paraiso")));
        when(shiftRepository.existsByStoreIdAndStatus(2L, "OPEN")).thenReturn(false);
        when(shiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShiftResponseDTO result = shiftService.openShift(2L, "cajero02", null);

        assertThat(result.getCode()).endsWith("-ELP");
    }

    @Test
    void openShift_codeContainsTodayDate() {
        String todayStr = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(shiftRepository.existsByStoreIdAndStatus(1L, "OPEN")).thenReturn(false);
        when(shiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShiftResponseDTO result = shiftService.openShift(1L, "cajero01", null);

        assertThat(result.getCode()).contains(todayStr);
    }

    // ── openShift — estado inicial ────────────────────────────────────────────

    @Test
    void openShift_setsStatusToOpen() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(shiftRepository.existsByStoreIdAndStatus(1L, "OPEN")).thenReturn(false);
        when(shiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShiftResponseDTO result = shiftService.openShift(1L, "cajero01", null);

        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.getUsername()).isEqualTo("cajero01");
    }

    // ── openShift — validaciones de negocio ───────────────────────────────────

    @Test
    void openShift_throwsWhenStoreNotFound() {
        when(storeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftService.openShift(99L, "cajero01", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Local no encontrado");
    }

    @Test
    void openShift_throwsWhenShiftAlreadyOpen() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danli")));
        when(shiftRepository.existsByStoreIdAndStatus(1L, "OPEN")).thenReturn(true);

        assertThatThrownBy(() -> shiftService.openShift(1L, "cajero01", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe un turno abierto");
    }

    // ── closeShift ────────────────────────────────────────────────────────────

    @Test
    void closeShift_setsStatusToClosedAndRegistersClosedAt() {
        Store store = buildStore(1L, "Danli");
        Shift shift = buildShift(1L, store, "OPEN");

        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift));
        when(shiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShiftResponseDTO result = shiftService.closeShift(1L);

        assertThat(result.getStatus()).isEqualTo("CLOSED");
        assertThat(shift.getClosedAt()).isNotNull();
    }

    @Test
    void closeShift_throwsWhenShiftNotFound() {
        when(shiftRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftService.closeShift(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Turno no encontrado");
    }

    @Test
    void closeShift_throwsWhenAlreadyClosed() {
        Store store = buildStore(1L, "Danli");
        Shift shift = buildShift(1L, store, "CLOSED");

        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> shiftService.closeShift(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya está cerrado");
    }

    // ── getActiveShift ────────────────────────────────────────────────────────

    @Test
    void getActiveShift_returnsNullWhenNoActiveShift() {
        when(shiftRepository.findByStoreIdAndStatus(1L, "OPEN")).thenReturn(Optional.empty());

        ShiftResponseDTO result = shiftService.getActiveShift(1L);

        assertThat(result).isNull();
    }

    @Test
    void getActiveShift_returnsShiftWhenExists() {
        Store store = buildStore(1L, "Danli");
        Shift shift = buildShift(1L, store, "OPEN");

        when(shiftRepository.findByStoreIdAndStatus(1L, "OPEN")).thenReturn(Optional.of(shift));

        ShiftResponseDTO result = shiftService.getActiveShift(1L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("OPEN");
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_throwsWhenNotFound() {
        when(shiftRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftService.getById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Turno no encontrado");
    }
}
