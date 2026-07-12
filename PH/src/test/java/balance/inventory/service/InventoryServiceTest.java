package balance.inventory.service;

import balance.catalog.model.Product;
import balance.catalog.repository.CategoryRepository;
import balance.catalog.repository.ProductRepository;
import balance.inventory.dto.StockAdjustmentDTO;
import balance.inventory.dto.StockItemDTO;
import balance.inventory.model.InventoryMovement;
import balance.inventory.model.InventoryStock;
import balance.inventory.repository.InventoryMovementRepository;
import balance.inventory.repository.InventoryStockRepository;
import balance.model.Store;
import balance.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @InjectMocks private InventoryService inventoryService;

    @Mock private InventoryStockRepository    stockRepository;
    @Mock private InventoryMovementRepository movementRepository;
    @Mock private ProductRepository           productRepository;
    @Mock private StoreRepository             storeRepository;
    @Mock private CategoryRepository          categoryRepository;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Store buildStore(Long id) {
        Store s = new Store();
        s.setId(id);
        s.setName("Danli");
        return s;
    }

    private Product buildProduct(Long id, int minStock) {
        Product p = new Product();
        p.setId(id);
        p.setName("Pollo");
        p.setPrice(new BigDecimal("100.00"));
        p.setMinStock(minStock);
        p.setActive(true);
        return p;
    }

    private InventoryStock buildStock(Product p, Store s, int qty) {
        InventoryStock stock = new InventoryStock();
        stock.setProduct(p);
        stock.setStore(s);
        stock.setQuantity(BigDecimal.valueOf(qty));
        return stock;
    }

    private StockAdjustmentDTO buildAdj(Long productId, String type, int qty) {
        StockAdjustmentDTO dto = new StockAdjustmentDTO();
        dto.setProductId(productId);
        dto.setType(type);
        dto.setQuantity(BigDecimal.valueOf(qty));
        dto.setReason("Test");
        dto.setUsername("admin");
        return dto;
    }

    // ── adjust — ENTRADA ──────────────────────────────────────────────────────

    @Test
    void adjust_ENTRADA_increasesQuantityCorrectly() {
        Store store = buildStore(1L);
        Product product = buildProduct(1L, 5);
        InventoryStock stock = buildStock(product, store, 10);

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndStoreId(1L, 1L)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.adjust(1L, buildAdj(1L, "ENTRADA", 5));

        assertThat(stock.getQuantity()).isEqualByComparingTo(new BigDecimal("15"));
        verify(stockRepository).save(stock);
    }

    // ── adjust — SALIDA ───────────────────────────────────────────────────────

    @Test
    void adjust_SALIDA_decreasesQuantityCorrectly() {
        Store store = buildStore(1L);
        Product product = buildProduct(1L, 0);
        InventoryStock stock = buildStock(product, store, 10);

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndStoreId(1L, 1L)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.adjust(1L, buildAdj(1L, "SALIDA", 3));

        assertThat(stock.getQuantity()).isEqualByComparingTo(new BigDecimal("7"));
    }

    @Test
    void adjust_SALIDA_throwsWhenStockInsufficient() {
        Store store = buildStore(1L);
        Product product = buildProduct(1L, 0);
        InventoryStock stock = buildStock(product, store, 2);

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndStoreId(1L, 1L)).thenReturn(Optional.of(stock));

        assertThatThrownBy(() -> inventoryService.adjust(1L, buildAdj(1L, "SALIDA", 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void adjust_SALIDA_throwsWhenQuantityExactlyExceedsStock() {
        // stock = 5, salida = 6 → debe fallar
        Store store = buildStore(1L);
        Product product = buildProduct(1L, 0);
        InventoryStock stock = buildStock(product, store, 5);

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndStoreId(1L, 1L)).thenReturn(Optional.of(stock));

        assertThatThrownBy(() -> inventoryService.adjust(1L, buildAdj(1L, "SALIDA", 6)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adjust_SALIDA_succeedsWhenQuantityEqualsStock() {
        // stock = 5, salida = 5 → debe dejar en 0 (válido)
        Store store = buildStore(1L);
        Product product = buildProduct(1L, 0);
        InventoryStock stock = buildStock(product, store, 5);

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndStoreId(1L, 1L)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.adjust(1L, buildAdj(1L, "SALIDA", 5));

        assertThat(stock.getQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── adjustSilent ──────────────────────────────────────────────────────────

    @Test
    void adjustSilent_doesNotThrowWhenStockInsufficient() {
        // Bug crítico corregido: con minStock=0 y quantity=0 no debe lanzar
        Store store = buildStore(1L);
        Product product = buildProduct(1L, 0);
        InventoryStock stock = buildStock(product, store, 0);

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndStoreId(1L, 1L)).thenReturn(Optional.of(stock));

        assertThatCode(() -> inventoryService.adjustSilent(1L, buildAdj(1L, "SALIDA", 999)))
                .doesNotThrowAnyException();
    }

    @Test
    void adjustSilent_doesNotSaveWhenStockInsufficient() {
        Store store = buildStore(1L);
        Product product = buildProduct(1L, 0);
        InventoryStock stock = buildStock(product, store, 1);

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndStoreId(1L, 1L)).thenReturn(Optional.of(stock));

        // Intentar sacar más de lo disponible — no debe guardar
        inventoryService.adjustSilent(1L, buildAdj(1L, "SALIDA", 999));

        verify(stockRepository, never()).save(any());
    }

    // ── movimiento registrado ─────────────────────────────────────────────────

    @Test
    void adjust_registersMovementWithCorrectFields() {
        Store store = buildStore(1L);
        Product product = buildProduct(1L, 0);
        InventoryStock stock = buildStock(product, store, 20);

        StockAdjustmentDTO dto = new StockAdjustmentDTO();
        dto.setProductId(1L);
        dto.setType("AJUSTE");
        dto.setQuantity(new BigDecimal("10"));
        dto.setReason("Conteo físico");
        dto.setNotes("Diferencia mensual");
        dto.setUsername("admin");

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndStoreId(1L, 1L)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.adjust(1L, dto);

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(captor.capture());
        InventoryMovement saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("AJUSTE");
        assertThat(saved.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(saved.getReason()).isEqualTo("Conteo físico");
        assertThat(saved.getNotes()).isEqualTo("Diferencia mensual");
        assertThat(saved.getUsername()).isEqualTo("admin");
    }

    @Test
    void adjust_alwaysRegistersMovementRegardlessOfType() {
        Store store = buildStore(1L);
        Product product = buildProduct(1L, 0);
        InventoryStock stock = buildStock(product, store, 10);

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndStoreId(1L, 1L)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.adjust(1L, buildAdj(1L, "ENTRADA", 5));

        // Siempre se registra movimiento, incluso para ENTRADA
        verify(movementRepository, times(1)).save(any(InventoryMovement.class));
    }

    // ── auto-crear stock ──────────────────────────────────────────────────────

    @Test
    void adjust_createsStockRecordIfNotExists() {
        Store store = buildStore(1L);
        Product product = buildProduct(1L, 0);

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndStoreId(1L, 1L)).thenReturn(Optional.empty());
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.adjust(1L, buildAdj(1L, "ENTRADA", 10));

        // stockRepository.save debe ser llamado para crear el registro nuevo con qty=10
        ArgumentCaptor<InventoryStock> captor = ArgumentCaptor.forClass(InventoryStock.class);
        verify(stockRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(s -> s.getQuantity().compareTo(BigDecimal.TEN) == 0);
    }

    // ── validaciones ──────────────────────────────────────────────────────────

    @Test
    void adjust_throwsWhenStoreNotFound() {
        when(storeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.adjust(99L, buildAdj(1L, "ENTRADA", 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Local no encontrado");
    }

    @Test
    void adjust_throwsWhenProductNotFound() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(buildStore(1L)));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.adjust(1L, buildAdj(99L, "ENTRADA", 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Producto no encontrado");
    }
}
