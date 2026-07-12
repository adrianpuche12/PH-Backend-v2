package balance.inventory.repository;

import balance.catalog.model.Product;
import balance.catalog.repository.ProductRepository;
import balance.inventory.model.InventoryStock;
import balance.model.Store;
import balance.repository.StoreRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(locations = "classpath:application-test.properties")
class InventoryStockRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired InventoryStockRepository stockRepository;
    @Autowired StoreRepository          storeRepository;
    @Autowired ProductRepository        productRepository;

    private Store store;

    @BeforeEach
    void setup() {
        stockRepository.deleteAll();
        productRepository.deleteAll();
        storeRepository.deleteAll();

        store = new Store();
        store.setName("Danli Test");
        store = storeRepository.save(store);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Product saveProduct(String name, int minStock) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(new BigDecimal("100.00"));
        p.setMinStock(minStock);
        p.setActive(true);
        p.setStore(store);
        return productRepository.save(p);
    }

    private InventoryStock saveStock(Product product, int quantity) {
        InventoryStock s = new InventoryStock();
        s.setProduct(product);
        s.setStore(store);
        s.setQuantity(java.math.BigDecimal.valueOf(quantity));
        return stockRepository.save(s);
    }

    // ── findLowStockByStoreId — lógica principal ──────────────────────────────

    @Test
    void findLowStockByStoreId_returnsProductBelowMinStock() {
        // minStock=5, quantity=3 → bajo stock ✓
        Product p = saveProduct("Pollo", 5);
        saveStock(p, 3);

        List<InventoryStock> result = stockRepository.findLowStockByStoreId(store.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProduct().getName()).isEqualTo("Pollo");
    }

    @Test
    void findLowStockByStoreId_excludesProductAboveMinStock() {
        // minStock=5, quantity=10 → stock OK, no debe aparecer
        Product p = saveProduct("Pollo", 5);
        saveStock(p, 10);

        List<InventoryStock> result = stockRepository.findLowStockByStoreId(store.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findLowStockByStoreId_excludesProductWithMinStockZero() {
        // Bug fix verificado: minStock=0, quantity=0 → NO es bajo stock
        // La query anterior (sin la condición minStock > 0) devolvía este producto
        Product p = saveProduct("Producto sin mínimo", 0);
        saveStock(p, 0);

        List<InventoryStock> result = stockRepository.findLowStockByStoreId(store.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findLowStockByStoreId_excludesProductWithMinStockZeroAndPositiveQuantity() {
        // minStock=0, quantity=5 → sin mínimo definido, no es bajo stock
        Product p = saveProduct("Producto sin mínimo con stock", 0);
        saveStock(p, 5);

        List<InventoryStock> result = stockRepository.findLowStockByStoreId(store.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findLowStockByStoreId_includesProductAtExactMinStock() {
        // minStock=5, quantity=5 → exactamente en el límite → bajo stock ✓
        Product p = saveProduct("Pollo en límite", 5);
        saveStock(p, 5);

        List<InventoryStock> result = stockRepository.findLowStockByStoreId(store.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    void findLowStockByStoreId_returnsOnlyLowStockAmongMultipleProducts() {
        // 3 productos: 1 bajo stock, 1 OK, 1 sin mínimo
        Product bajo   = saveProduct("Bajo stock",   5); saveStock(bajo,   2);
        Product ok     = saveProduct("OK",           5); saveStock(ok,     10);
        Product sinMin = saveProduct("Sin mínimo",   0); saveStock(sinMin, 0);

        List<InventoryStock> result = stockRepository.findLowStockByStoreId(store.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProduct().getName()).isEqualTo("Bajo stock");
    }

    @Test
    void findLowStockByStoreId_onlyReturnsProductsForRequestedStore() {
        // Crear segundo local con producto bajo stock — no debe aparecer
        Store otraStore = new Store();
        otraStore.setName("El Paraíso Test");
        otraStore = storeRepository.save(otraStore);

        Product pOtra = new Product();
        pOtra.setName("Pollo Paraíso");
        pOtra.setPrice(new BigDecimal("100.00"));
        pOtra.setMinStock(5);
        pOtra.setActive(true);
        pOtra.setStore(otraStore);
        pOtra = productRepository.save(pOtra);

        InventoryStock sOtra = new InventoryStock();
        sOtra.setProduct(pOtra);
        sOtra.setStore(otraStore);
        sOtra.setQuantity(java.math.BigDecimal.ONE);
        stockRepository.save(sOtra);

        // El local principal no tiene productos bajo stock
        List<InventoryStock> result = stockRepository.findLowStockByStoreId(store.getId());

        assertThat(result).isEmpty();
    }

    // ── countLowStockByStoreId ────────────────────────────────────────────────

    @Test
    void countLowStockByStoreId_returnsCorrectCount() {
        Product p1 = saveProduct("Bajo1", 5); saveStock(p1, 1);
        Product p2 = saveProduct("Bajo2", 5); saveStock(p2, 2);
        Product p3 = saveProduct("OK",    5); saveStock(p3, 10);

        long count = stockRepository.countLowStockByStoreId(store.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countLowStockByStoreId_returnsZeroWhenNoLowStock() {
        Product p = saveProduct("OK", 5);
        saveStock(p, 10);

        long count = stockRepository.countLowStockByStoreId(store.getId());

        assertThat(count).isZero();
    }

    // ── findByProductIdAndStoreId ─────────────────────────────────────────────

    @Test
    void findByProductIdAndStoreId_returnsStockWhenExists() {
        Product p = saveProduct("Pollo", 5);
        InventoryStock saved = saveStock(p, 10);

        Optional<InventoryStock> result =
                stockRepository.findByProductIdAndStoreId(p.getId(), store.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getQuantity()).isEqualTo(10);
    }

    @Test
    void findByProductIdAndStoreId_returnsEmptyWhenNotExists() {
        Optional<InventoryStock> result =
                stockRepository.findByProductIdAndStoreId(999L, store.getId());

        assertThat(result).isEmpty();
    }

    // ── findByStoreIdOrderByProductNameAsc ────────────────────────────────────

    @Test
    void findByStoreIdOrderByProductNameAsc_returnsInAlphabeticOrder() {
        Product pZ = saveProduct("Zapallo",  0); saveStock(pZ, 5);
        Product pA = saveProduct("Ala",      0); saveStock(pA, 3);
        Product pM = saveProduct("Muslo",    0); saveStock(pM, 8);

        List<InventoryStock> result =
                stockRepository.findByStoreIdOrderByProductNameAsc(store.getId());

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getProduct().getName()).isEqualTo("Ala");
        assertThat(result.get(1).getProduct().getName()).isEqualTo("Muslo");
        assertThat(result.get(2).getProduct().getName()).isEqualTo("Zapallo");
    }
}
