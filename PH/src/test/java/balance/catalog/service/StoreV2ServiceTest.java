package balance.catalog.service;

import balance.catalog.dto.StoreRequestDTO;
import balance.catalog.dto.StoreResponseDTO;
import balance.catalog.repository.CategoryRepository;
import balance.catalog.repository.ProductRepository;
import balance.inventory.repository.InventoryMovementRepository;
import balance.inventory.repository.InventoryStockRepository;
import balance.model.Store;
import balance.repository.ClosingDepositRepository;
import balance.repository.SalaryPaymentRepository;
import balance.repository.StoreRepository;
import balance.repository.SupplierPaymentRepository;
import balance.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreV2ServiceTest {

    @InjectMocks private StoreV2Service service;

    @Mock private StoreRepository          storeRepository;
    @Mock private ProductRepository        productRepository;
    @Mock private CategoryRepository       categoryRepository;
    @Mock private InventoryStockRepository inventoryStockRepository;
    @Mock private InventoryMovementRepository inventoryMovementRepository;
    @Mock private TransactionRepository    transactionRepository;
    @Mock private ClosingDepositRepository closingDepositRepository;
    @Mock private SupplierPaymentRepository supplierPaymentRepository;
    @Mock private SalaryPaymentRepository  salaryPaymentRepository;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Store buildStore(Long id, String name, boolean active) {
        Store s = new Store();
        s.setId(id);
        s.setName(name);
        s.setActive(active);
        return s;
    }

    private StoreRequestDTO buildRequest(String name) {
        StoreRequestDTO dto = new StoreRequestDTO();
        dto.setName(name);
        dto.setAddress("Calle 1");
        return dto;
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_returnsAllStores() {
        when(storeRepository.findAll())
                .thenReturn(List.of(buildStore(1L, "Danlí", true), buildStore(2L, "El Paraíso", false)));

        List<StoreResponseDTO> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(StoreResponseDTO::getName)
                .containsExactly("Danlí", "El Paraíso");
    }

    @Test
    void findAll_returnsEmptyListWhenNoStores() {
        when(storeRepository.findAll()).thenReturn(List.of());
        assertThat(service.findAll()).isEmpty();
    }

    // ── findAllActive ─────────────────────────────────────────────────────────

    @Test
    void findAllActive_returnsOnlyActiveStores() {
        when(storeRepository.findAll())
                .thenReturn(List.of(buildStore(1L, "Activo", true), buildStore(2L, "Inactivo", false)));

        List<StoreResponseDTO> result = service.findAllActive();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Activo");
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_returnsStoreWhenFound() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L, "Danlí", true)));

        Optional<StoreResponseDTO> result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Danlí");
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(storeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.findById(99L)).isEmpty();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturnsStore() {
        Store saved = buildStore(10L, "Nueva Sucursal", true);
        when(storeRepository.save(any())).thenReturn(saved);

        StoreResponseDTO result = service.create(buildRequest("Nueva Sucursal"));

        assertThat(result.getName()).isEqualTo("Nueva Sucursal");
        verify(storeRepository).save(any());
    }

    @Test
    void create_trimsWhitespacesFromName() {
        Store saved = buildStore(10L, "Nombre Limpio", true);
        when(storeRepository.save(any())).thenReturn(saved);

        StoreRequestDTO req = buildRequest("  Nombre Limpio  ");
        service.create(req);

        verify(storeRepository).save(argThat(s -> "Nombre Limpio".equals(s.getName())));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_updatesFieldsAndReturnsDTO() {
        Store existing = buildStore(1L, "Viejo", true);
        Store updated  = buildStore(1L, "Nuevo", true);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(storeRepository.save(any())).thenReturn(updated);

        Optional<StoreResponseDTO> result = service.update(1L, buildRequest("Nuevo"));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Nuevo");
    }

    @Test
    void update_returnsEmptyWhenStoreNotFound() {
        when(storeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.update(99L, buildRequest("X"))).isEmpty();
    }

    // ── toggle ────────────────────────────────────────────────────────────────

    @Test
    void toggle_deactivatesActiveStore() {
        Store store = buildStore(1L, "Danlí", true);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<StoreResponseDTO> result = service.toggle(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getActive()).isFalse();
    }

    @Test
    void toggle_activatesInactiveStore() {
        Store store = buildStore(1L, "Danlí", false);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<StoreResponseDTO> result = service.toggle(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getActive()).isTrue();
    }

    @Test
    void toggle_returnsEmptyWhenStoreNotFound() {
        when(storeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.toggle(99L)).isEmpty();
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_returnsFalseWhenStoreNotFound() {
        when(storeRepository.existsById(99L)).thenReturn(false);
        assertThat(service.delete(99L)).isFalse();
    }

    @Test
    void delete_throwsWhenStoreHasOperationalHistory() {
        when(storeRepository.existsById(1L)).thenReturn(true);
        when(transactionRepository.findByStoreId(1L)).thenReturn(List.of(new balance.model.Transaction()));

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("historial de operaciones");
    }

    @Test
    void delete_deletesStoreWithNoHistory() {
        when(storeRepository.existsById(1L)).thenReturn(true);
        when(transactionRepository.findByStoreId(1L)).thenReturn(List.of());
        when(closingDepositRepository.findByStoreIdOrderByDepositDateDesc(1L)).thenReturn(List.of());
        when(supplierPaymentRepository.findByStoreIdOrderByPaymentDateDesc(1L)).thenReturn(List.of());
        when(salaryPaymentRepository.findByStoreIdOrderBySalaryDateDesc(1L)).thenReturn(List.of());
        when(productRepository.findByStoreIdOrderByNameAsc(1L)).thenReturn(List.of());
        when(categoryRepository.findRootsByStoreId(1L)).thenReturn(List.of());

        boolean result = service.delete(1L);

        assertThat(result).isTrue();
        verify(storeRepository).deleteById(1L);
    }
}
