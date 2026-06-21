package balance.sales.service;

import balance.catalog.model.Product;
import balance.catalog.repository.ProductRepository;
import balance.inventory.dto.StockAdjustmentDTO;
import balance.inventory.service.InventoryService;
import balance.model.Store;
import balance.repository.StoreRepository;
import balance.sales.dto.SaleItemRequestDTO;
import balance.sales.dto.SaleRequestDTO;
import balance.sales.dto.SaleResponseDTO;
import balance.sales.dto.ShiftExpenseRequestDTO;
import balance.sales.model.Sale;
import balance.sales.model.SaleItem;
import balance.sales.model.Shift;
import balance.sales.model.ShiftExpense;
import balance.sales.repository.SaleRepository;
import balance.sales.repository.ShiftExpenseRepository;
import balance.sales.repository.ShiftRepository;
import balance.service.FormsService;
import balance.model.ClosingDeposit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    @InjectMocks private SalesService salesService;

    @Mock private SaleRepository           saleRepository;
    @Mock private ShiftRepository          shiftRepository;
    @Mock private ShiftExpenseRepository   shiftExpenseRepository;
    @Mock private ProductRepository        productRepository;
    @Mock private StoreRepository          storeRepository;
    @Mock private InventoryService         inventoryService;
    @Mock private FormsService             formsService;
    @Mock private balance.inventory.repository.InventoryStockRepository inventoryStockRepository;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Store buildStore(Long id, String name) {
        Store s = new Store();
        s.setId(id);
        s.setName(name);
        return s;
    }

    private Shift buildShift(Long id, String status) {
        Shift shift = new Shift();
        shift.setStore(buildStore(1L, "Danli"));
        shift.setStatus(status);
        shift.setCode("T-20260514-0900-DAN");
        shift.setUsername("cajero01");
        return shift;
    }

    private Product buildProduct(Long id, String name, BigDecimal price) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(price);
        p.setActive(true);
        return p;
    }

    private SaleRequestDTO buildRequest(String username, Long productId, int qty) {
        SaleItemRequestDTO item = new SaleItemRequestDTO();
        item.setProductId(productId);
        item.setQuantity(qty);
        SaleRequestDTO req = new SaleRequestDTO();
        req.setUsername(username);
        req.setItems(List.of(item));
        return req;
    }

    // ── createSale — cálculo financiero ───────────────────────────────────────

    @Test
    void createSale_calculatesIsvCorrectly() {
        // ISV deshabilitado (ISV_RATE = 0) — subtotal = 100 * 2 = 200.00, ISV = 0.00, total = 200.00
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(buildShift(1L, "OPEN")));
        when(productRepository.findById(1L)).thenReturn(Optional.of(buildProduct(1L, "Pollo", new BigDecimal("100.00"))));
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDTO result = salesService.createSale(1L, buildRequest("cajero01", 1L, 2));

        assertThat(result.getSubtotal()).isEqualByComparingTo("200.00");
        assertThat(result.getIsv()).isEqualByComparingTo("0.00");
        assertThat(result.getTotal()).isEqualByComparingTo("200.00");
    }

    @Test
    void createSale_isvIsZeroWhenDisabled() {
        // ISV deshabilitado por solicitud del cliente — siempre 0, total = subtotal
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(buildShift(1L, "OPEN")));
        when(productRepository.findById(1L)).thenReturn(Optional.of(buildProduct(1L, "Ala", new BigDecimal("33.33"))));
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDTO result = salesService.createSale(1L, buildRequest("cajero01", 1L, 3));

        assertThat(result.getIsv()).isEqualByComparingTo("0.00");
        assertThat(result.getTotal()).isEqualByComparingTo(result.getSubtotal());
    }

    // ── createSale — recargo 2% por pago con tarjeta ───────────────────────────

    @Test
    void createSale_appliesCardSurchargeForCardPayment() {
        // Pago con tarjeta: recargo del 2% sobre el total base -> 100.00 * 1.02 = 102.00
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(buildShift(1L, "OPEN")));
        when(productRepository.findById(1L)).thenReturn(Optional.of(buildProduct(1L, "Pollo", new BigDecimal("100.00"))));
        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        when(saleRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        SaleRequestDTO req = buildRequest("cajero01", 1L, 1);
        req.setPaymentMethod("CARD");

        SaleResponseDTO result = salesService.createSale(1L, req);

        assertThat(result.getSubtotal()).isEqualByComparingTo("100.00");
        assertThat(result.getTotal()).isEqualByComparingTo("102.00");
        assertThat(captor.getValue().getCardAmount()).isEqualByComparingTo("102.00");
        assertThat(captor.getValue().getCashAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void createSale_appliesCardSurchargeOnlyToCardPortionInMixedPayment() {
        // Total base = 100.00. Split: efectivo 60, tarjeta 40 -> recargo 2% de 40 = 0.80
        // total final = 100.80, cardAmount = 40.80, cashAmount = 60.00
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(buildShift(1L, "OPEN")));
        when(productRepository.findById(1L)).thenReturn(Optional.of(buildProduct(1L, "Pollo", new BigDecimal("100.00"))));
        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        when(saleRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        SaleRequestDTO req = buildRequest("cajero01", 1L, 1);
        req.setPaymentMethod("MIXED");
        req.setCashAmount(new BigDecimal("60.00"));
        req.setCardAmount(new BigDecimal("40.00"));

        SaleResponseDTO result = salesService.createSale(1L, req);

        assertThat(result.getTotal()).isEqualByComparingTo("100.80");
        assertThat(captor.getValue().getCashAmount()).isEqualByComparingTo("60.00");
        assertThat(captor.getValue().getCardAmount()).isEqualByComparingTo("40.80");
    }

    @Test
    void createSale_mixedValidatesAgainstBaseTotalNotFinalTotal() {
        // efectivo + tarjeta deben sumar el total BASE (100.00), no el total con recargo ya incluido
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(buildShift(1L, "OPEN")));
        when(productRepository.findById(1L)).thenReturn(Optional.of(buildProduct(1L, "Pollo", new BigDecimal("100.00"))));

        SaleRequestDTO req = buildRequest("cajero01", 1L, 1);
        req.setPaymentMethod("MIXED");
        req.setCashAmount(new BigDecimal("60.00"));
        req.setCardAmount(new BigDecimal("41.20")); // ya incluiría el recargo -> no coincide con el total base (100.00)

        assertThatThrownBy(() -> salesService.createSale(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("efectivo + tarjeta debe ser igual al total");
    }

    // ── createSale — snapshot de producto ─────────────────────────────────────

    @Test
    void createSale_savesProductNameAndPriceSnapshot() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(buildShift(1L, "OPEN")));
        when(productRepository.findById(1L)).thenReturn(Optional.of(buildProduct(1L, "Pollo Entero", new BigDecimal("150.00"))));
        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        when(saleRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        salesService.createSale(1L, buildRequest("cajero01", 1L, 1));

        SaleItem savedItem = captor.getValue().getItems().get(0);
        assertThat(savedItem.getProductNameSnapshot()).isEqualTo("Pollo Entero");
        assertThat(savedItem.getUnitPriceSnapshot()).isEqualByComparingTo("150.00");
    }

    // ── createSale — validaciones de negocio ──────────────────────────────────

    @Test
    void createSale_throwsWhenShiftNotFound() {
        when(shiftRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesService.createSale(99L, buildRequest("cajero", 1L, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Turno no encontrado");
    }

    @Test
    void createSale_throwsWhenShiftIsClosed() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(buildShift(1L, "CLOSED")));

        assertThatThrownBy(() -> salesService.createSale(1L, buildRequest("cajero", 1L, 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("turno ya está cerrado");
    }

    @Test
    void createSale_throwsWhenProductNotFound() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(buildShift(1L, "OPEN")));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesService.createSale(1L, buildRequest("cajero", 99L, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Producto no encontrado");
    }

    @Test
    void createSale_throwsWhenProductIsInactive() {
        Product inactivo = buildProduct(1L, "Pollo", new BigDecimal("100.00"));
        inactivo.setActive(false);
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(buildShift(1L, "OPEN")));
        when(productRepository.findById(1L)).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> salesService.createSale(1L, buildRequest("cajero", 1L, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Producto inactivo");
    }

    // ── createSale — stock ────────────────────────────────────────────────────

    @Test
    void createSale_callsAdjustSilentForEachItem() {
        SaleItemRequestDTO i1 = new SaleItemRequestDTO(); i1.setProductId(1L); i1.setQuantity(2);
        SaleItemRequestDTO i2 = new SaleItemRequestDTO(); i2.setProductId(2L); i2.setQuantity(1);
        SaleRequestDTO req = new SaleRequestDTO();
        req.setUsername("cajero");
        req.setItems(List.of(i1, i2));

        when(shiftRepository.findById(1L)).thenReturn(Optional.of(buildShift(1L, "OPEN")));
        when(productRepository.findById(1L)).thenReturn(Optional.of(buildProduct(1L, "Pollo", new BigDecimal("100.00"))));
        when(productRepository.findById(2L)).thenReturn(Optional.of(buildProduct(2L, "Ala", new BigDecimal("50.00"))));
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        salesService.createSale(1L, req);

        // Una llamada adjustSilent por cada producto vendido
        verify(inventoryService, times(2)).adjustSilent(eq(1L), any(StockAdjustmentDTO.class));
    }

    @Test
    void createSale_adjustSilentUsesTipoSalida() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(buildShift(1L, "OPEN")));
        when(productRepository.findById(1L)).thenReturn(Optional.of(buildProduct(1L, "Pollo", new BigDecimal("100.00"))));
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<StockAdjustmentDTO> captor = ArgumentCaptor.forClass(StockAdjustmentDTO.class);

        salesService.createSale(1L, buildRequest("cajero", 1L, 3));

        verify(inventoryService).adjustSilent(eq(1L), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("SALIDA");
        assertThat(captor.getValue().getQuantity()).isEqualTo(3);
    }

    // ── cancelSale ────────────────────────────────────────────────────────────

    @Test
    void cancelSale_deletesOpenSale() {
        Sale sale = new Sale();
        sale.setStatus("OPEN");
        sale.setStore(buildStore(1L, "Danli"));

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));

        salesService.cancelSale(1L);

        verify(saleRepository).delete(sale);
    }

    @Test
    void cancelSale_revertsStockWithEntrada() {
        Product product = buildProduct(1L, "Pollo", new BigDecimal("100.00"));
        SaleItem item = new SaleItem();
        item.setProduct(product);
        item.setQuantity(3);
        item.setProductNameSnapshot("Pollo");
        item.setUnitPriceSnapshot(new BigDecimal("100.00"));
        item.setSubtotal(new BigDecimal("300.00"));

        Sale sale = new Sale();
        sale.setStatus("OPEN");
        sale.setStore(buildStore(1L, "Danli"));
        sale.getItems().add(item);

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        ArgumentCaptor<StockAdjustmentDTO> captor = ArgumentCaptor.forClass(StockAdjustmentDTO.class);

        salesService.cancelSale(1L);

        verify(inventoryService).adjustSilent(eq(1L), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("ENTRADA");
        assertThat(captor.getValue().getQuantity()).isEqualTo(3);
    }

    @Test
    void cancelSale_throwsWhenSaleNotFound() {
        when(saleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesService.cancelSale(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Venta no encontrada");
    }

    @Test
    void cancelSale_throwsWhenSaleIsConfirmed() {
        Sale sale = new Sale();
        sale.setStatus("CONFIRMED");
        sale.setStore(buildStore(1L, "Danli"));

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> salesService.cancelSale(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede cancelar");
    }

    // ── updateSale ───────────────────────────────────────────────────────────

    private Sale buildOpenSaleWithItem(Shift shift, String username, Product product, int qty, BigDecimal unitPrice) {
        Sale sale = new Sale();
        sale.setStatus("OPEN");
        sale.setUsername(username);
        sale.setShift(shift);
        sale.setStore(shift.getStore());
        SaleItem item = new SaleItem();
        item.setProduct(product);
        item.setProductNameSnapshot(product.getName());
        item.setUnitPriceSnapshot(unitPrice);
        item.setQuantity(qty);
        item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(qty)));
        sale.getItems().add(item);
        sale.setSubtotal(item.getSubtotal());
        sale.setTotal(item.getSubtotal());
        return sale;
    }

    @Test
    void updateSale_replacesItemsAndRecalculatesTotal() {
        // Venta original: 4 pollos a L100 = L400. Se edita a 2 pollos = L200.
        Shift shift = buildShift(1L, "OPEN");
        Product product = buildProduct(1L, "Pollo", new BigDecimal("100.00"));
        Sale sale = buildOpenSaleWithItem(shift, "cajero01", product, 4, new BigDecimal("100.00"));

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDTO result = salesService.updateSale(1L, buildRequest("cajero01", 1L, 2));

        assertThat(result.getSubtotal()).isEqualByComparingTo("200.00");
        assertThat(result.getTotal()).isEqualByComparingTo("200.00");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void updateSale_revertsOldStockAndDeductsNewStock() {
        // 4 pollos vendidos -> se edita a 2: debe revertir 4 (ENTRADA) y descontar 2 (SALIDA)
        Shift shift = buildShift(1L, "OPEN");
        Product product = buildProduct(1L, "Pollo", new BigDecimal("100.00"));
        Sale sale = buildOpenSaleWithItem(shift, "cajero01", product, 4, new BigDecimal("100.00"));

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        salesService.updateSale(1L, buildRequest("cajero01", 1L, 2));

        ArgumentCaptor<StockAdjustmentDTO> captor = ArgumentCaptor.forClass(StockAdjustmentDTO.class);
        verify(inventoryService, times(2)).adjustSilent(eq(1L), captor.capture());

        List<StockAdjustmentDTO> calls = captor.getAllValues();
        assertThat(calls.get(0).getType()).isEqualTo("ENTRADA");
        assertThat(calls.get(0).getQuantity()).isEqualTo(4);
        assertThat(calls.get(1).getType()).isEqualTo("SALIDA");
        assertThat(calls.get(1).getQuantity()).isEqualTo(2);
    }

    @Test
    void updateSale_marksSaleAsEdited() {
        Shift shift = buildShift(1L, "OPEN");
        Product product = buildProduct(1L, "Pollo", new BigDecimal("100.00"));
        Sale sale = buildOpenSaleWithItem(shift, "cajero01", product, 4, new BigDecimal("100.00"));

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDTO result = salesService.updateSale(1L, buildRequest("cajero01", 1L, 2));

        assertThat(result.getEdited()).isTrue();
        assertThat(result.getEditedAt()).isNotNull();
    }

    @Test
    void updateSale_throwsWhenSaleNotFound() {
        when(saleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesService.updateSale(99L, buildRequest("cajero01", 1L, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Venta no encontrada");
    }

    @Test
    void updateSale_throwsWhenShiftClosed() {
        Shift shift = buildShift(1L, "CLOSED");
        Product product = buildProduct(1L, "Pollo", new BigDecimal("100.00"));
        Sale sale = buildOpenSaleWithItem(shift, "cajero01", product, 4, new BigDecimal("100.00"));

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> salesService.updateSale(1L, buildRequest("cajero01", 1L, 2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("turno cerrado");
    }

    @Test
    void updateSale_throwsWhenSaleAlreadyConfirmed() {
        Shift shift = buildShift(1L, "OPEN");
        Product product = buildProduct(1L, "Pollo", new BigDecimal("100.00"));
        Sale sale = buildOpenSaleWithItem(shift, "cajero01", product, 4, new BigDecimal("100.00"));
        sale.setStatus("CONFIRMED");

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> salesService.updateSale(1L, buildRequest("cajero01", 1L, 2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya confirmada");
    }

    @Test
    void updateSale_throwsWhenUsernameDoesNotMatchOwner() {
        Shift shift = buildShift(1L, "OPEN");
        Product product = buildProduct(1L, "Pollo", new BigDecimal("100.00"));
        Sale sale = buildOpenSaleWithItem(shift, "cajero01", product, 4, new BigDecimal("100.00"));

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> salesService.updateSale(1L, buildRequest("otraCajera", 1L, 2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("otro usuario");
    }

    // ── closeShift ────────────────────────────────────────────────────────────

    @Test
    void closeShift_throwsWhenShiftAlreadyClosed() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(buildShift(1L, "CLOSED")));

        assertThatThrownBy(() -> salesService.closeShift(1L, "admin", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("turno ya está cerrado");
    }

    @Test
    void closeShift_succeedsWithZeroSales() {
        // Un turno sin ventas es valido (cajero abrio pero no hubo clientes)
        Shift shift = buildShift(1L, "OPEN");
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift));
        when(saleRepository.findOpenByShiftId(1L)).thenReturn(List.of());
        when(shiftExpenseRepository.sumAmountByShiftId(1L)).thenReturn(BigDecimal.ZERO);
        when(shiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // No debe lanzar excepcion, no debe crear ClosingDeposit (amount/closingsCount serian 0,
        // lo que violaria @DecimalMin/@Min en ClosingDeposit) y closingDepositId queda null
        var result = salesService.closeShift(1L, "admin", BigDecimal.ZERO);

        verify(formsService, never()).saveClosingDeposit(any());
        assertThat(result.getClosingDepositId()).isNull();
        assertThat(shift.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    void closeShift_confirmsAllOpenSalesAndSavesShift() {
        Shift shift = buildShift(1L, "OPEN");
        Sale sale1 = new Sale(); sale1.setStatus("OPEN");
        sale1.setTotal(new BigDecimal("115.00"));
        sale1.setStore(buildStore(1L, "Danli"));
        Sale sale2 = new Sale(); sale2.setStatus("OPEN");
        sale2.setTotal(new BigDecimal("230.00"));
        sale2.setStore(buildStore(1L, "Danli"));

        ClosingDeposit deposit = new ClosingDeposit();
        deposit.setId(10L);

        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift));
        when(saleRepository.findOpenByShiftId(1L)).thenReturn(List.of(sale1, sale2));
        when(shiftExpenseRepository.sumAmountByShiftId(1L)).thenReturn(BigDecimal.ZERO);
        when(formsService.saveClosingDeposit(any())).thenReturn(deposit);
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        salesService.closeShift(1L, "admin", null);

        assertThat(sale1.getStatus()).isEqualTo("CONFIRMED");
        assertThat(sale2.getStatus()).isEqualTo("CONFIRMED");
        assertThat(shift.getStatus()).isEqualTo("CLOSED");
        assertThat(shift.getClosedAt()).isNotNull();
    }

    @Test
    void closeShift_cashDifferenceExcludesFondoInicial() {
        // El fondo inicial (L 1000) NO debe sumarse al esperado.
        // Cajera con L 1000 fondo + L 20 ventas efectivo declara solo L 20 → caja cuadrada.
        Shift shift = buildShift(1L, "OPEN");
        shift.setOpeningCashAmount(new BigDecimal("1000.00"));

        Sale sale = new Sale();
        sale.setStatus("OPEN");
        sale.setTotal(new BigDecimal("20.00"));
        sale.setCashAmount(new BigDecimal("20.00"));
        sale.setCardAmount(BigDecimal.ZERO);
        sale.setSubtotal(new BigDecimal("20.00"));
        sale.setIsv(BigDecimal.ZERO);
        sale.setStore(buildStore(1L, "Danli"));

        ClosingDeposit deposit = new ClosingDeposit(); deposit.setId(1L);

        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift));
        when(saleRepository.findOpenByShiftId(1L)).thenReturn(List.of(sale));
        when(shiftExpenseRepository.sumAmountByShiftId(1L)).thenReturn(BigDecimal.ZERO);
        when(formsService.saveClosingDeposit(any())).thenReturn(deposit);
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // La cajera declara L 20 (solo ventas, sin contar el fondo)
        var result = salesService.closeShift(1L, "admin", new BigDecimal("20.00"));

        assertThat(result.getExpectedCashAmount()).isEqualByComparingTo("20.00");
        assertThat(result.getCashDifference()).isEqualByComparingTo("0.00");
    }

    // ── updateExpense / deleteExpense ──────────────────────────────────────────

    private ShiftExpense buildExpense(Shift shift, String description, BigDecimal amount) {
        ShiftExpense expense = new ShiftExpense();
        expense.setShift(shift);
        expense.setDescription(description);
        expense.setAmount(amount);
        expense.setUsername("cajero01");
        return expense;
    }

    private ShiftExpenseRequestDTO buildExpenseRequest(String description, BigDecimal amount) {
        ShiftExpenseRequestDTO dto = new ShiftExpenseRequestDTO();
        dto.setDescription(description);
        dto.setAmount(amount);
        return dto;
    }

    @Test
    void updateExpense_updatesDescriptionAndAmountWhenShiftOpen() {
        Shift shift = buildShift(1L, "OPEN");
        ShiftExpense expense = buildExpense(shift, "Tortillas maíz", new BigDecimal("50.00"));

        when(shiftExpenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(shiftExpenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = salesService.updateExpense(1L, buildExpenseRequest("Gas", new BigDecimal("75.00")));

        assertThat(expense.getDescription()).isEqualTo("Gas");
        assertThat(expense.getAmount()).isEqualByComparingTo("75.00");
        assertThat(result.getDescription()).isEqualTo("Gas");
        assertThat(result.getAmount()).isEqualByComparingTo("75.00");
    }

    @Test
    void updateExpense_throwsWhenExpenseNotFound() {
        when(shiftExpenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesService.updateExpense(99L, buildExpenseRequest("Gas", new BigDecimal("75.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Egreso no encontrado");
    }

    @Test
    void updateExpense_throwsWhenShiftIsClosed() {
        Shift shift = buildShift(1L, "CLOSED");
        ShiftExpense expense = buildExpense(shift, "Tortillas maíz", new BigDecimal("50.00"));

        when(shiftExpenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> salesService.updateExpense(1L, buildExpenseRequest("Gas", new BigDecimal("75.00"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede editar");
    }

    @Test
    void deleteExpense_deletesWhenShiftOpen() {
        Shift shift = buildShift(1L, "OPEN");
        ShiftExpense expense = buildExpense(shift, "Tortillas maíz", new BigDecimal("50.00"));

        when(shiftExpenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        salesService.deleteExpense(1L);

        verify(shiftExpenseRepository).delete(expense);
    }

    @Test
    void deleteExpense_throwsWhenExpenseNotFound() {
        when(shiftExpenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesService.deleteExpense(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Egreso no encontrado");
    }

    @Test
    void deleteExpense_throwsWhenShiftIsClosed() {
        Shift shift = buildShift(1L, "CLOSED");
        ShiftExpense expense = buildExpense(shift, "Tortillas maíz", new BigDecimal("50.00"));

        when(shiftExpenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> salesService.deleteExpense(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede eliminar");

        verify(shiftExpenseRepository, never()).delete(any());
    }
}
