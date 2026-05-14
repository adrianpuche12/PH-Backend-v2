package balance.sales.service;

import balance.catalog.model.Product;
import balance.catalog.repository.ProductRepository;
import balance.inventory.dto.StockAdjustmentDTO;
import balance.inventory.service.InventoryService;
import balance.model.ClosingDeposit;
import balance.model.Store;
import balance.repository.StoreRepository;
import balance.sales.dto.*;
import balance.sales.model.Sale;
import balance.sales.model.SaleItem;
import balance.sales.model.Shift;
import balance.sales.repository.SaleRepository;
import balance.sales.repository.ShiftRepository;
import balance.service.FormsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesService {

    private static final BigDecimal ISV_RATE = new BigDecimal("0.15");

    @Autowired private SaleRepository saleRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private InventoryService inventoryService;
    @Autowired private FormsService formsService;

    // ── Crear venta ──────────────────────────────────────────────────────────

    @Transactional
    public SaleResponseDTO createSale(Long shiftId, SaleRequestDTO request) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));

        if ("CLOSED".equals(shift.getStatus())) {
            throw new IllegalStateException("El turno ya está cerrado");
        }

        Store store = shift.getStore();

        Sale sale = new Sale();
        sale.setShift(shift);
        sale.setStore(store);
        sale.setUsername(request.getUsername());
        sale.setSaleDate(LocalDate.now());
        sale.setStatus("OPEN");

        BigDecimal subtotal = BigDecimal.ZERO;

        for (SaleItemRequestDTO itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + itemReq.getProductId()));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException("Producto inactivo: " + product.getName());
            }

            BigDecimal itemSubtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setProduct(product);
            item.setProductNameSnapshot(product.getName());
            item.setUnitPriceSnapshot(product.getPrice());
            item.setQuantity(itemReq.getQuantity());
            item.setSubtotal(itemSubtotal);

            sale.getItems().add(item);
            subtotal = subtotal.add(itemSubtotal);
        }

        BigDecimal isv   = subtotal.multiply(ISV_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(isv);

        sale.setSubtotal(subtotal);
        sale.setIsv(isv);
        sale.setTotal(total);

        saleRepository.save(sale);

        // Descontar stock de cada producto vendido
        deductStock(store.getId(), sale.getItems());

        return SaleResponseDTO.from(sale);
    }

    // ── Cancelar venta ───────────────────────────────────────────────────────

    @Transactional
    public void cancelSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada"));

        if ("CONFIRMED".equals(sale.getStatus())) {
            throw new IllegalStateException("No se puede cancelar una venta ya confirmada");
        }

        // Revertir stock
        revertStock(sale.getStore().getId(), sale.getItems());

        saleRepository.delete(sale);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public List<SaleResponseDTO> getSalesByShift(Long shiftId) {
        return saleRepository.findByShiftIdOrderByCreatedAtDesc(shiftId)
                .stream().map(SaleResponseDTO::from).toList();
    }

    public SaleResponseDTO getSaleById(Long saleId) {
        return saleRepository.findById(saleId)
                .map(SaleResponseDTO::from)
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada"));
    }

    // ── Resumen diario ────────────────────────────────────────────────────────

    public DailySummaryDTO getDailySummary(Long shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));

        List<Sale> openSales = saleRepository.findOpenByShiftId(shiftId);

        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalIsv      = BigDecimal.ZERO;
        BigDecimal totalAmount   = BigDecimal.ZERO;

        // Agregar por producto (usando nombre del snapshot)
        Map<String, int[]> productQty       = new LinkedHashMap<>();
        Map<String, BigDecimal> productSub  = new LinkedHashMap<>();
        Map<String, Long> productIds        = new LinkedHashMap<>();

        for (Sale sale : openSales) {
            totalSubtotal = totalSubtotal.add(sale.getSubtotal());
            totalIsv      = totalIsv.add(sale.getIsv());
            totalAmount   = totalAmount.add(sale.getTotal());

            for (SaleItem item : sale.getItems()) {
                String key = item.getProductNameSnapshot();
                productQty.merge(key, new int[]{item.getQuantity()}, (a, b) -> new int[]{a[0] + b[0]});
                productSub.merge(key, item.getSubtotal(), BigDecimal::add);
                if (item.getProduct() != null) productIds.putIfAbsent(key, item.getProduct().getId());
            }
        }

        List<DailySummaryDTO.ProductSummaryItem> summary = productQty.entrySet().stream()
                .map(e -> new DailySummaryDTO.ProductSummaryItem(
                        productIds.get(e.getKey()),
                        e.getKey(),
                        e.getValue()[0],
                        productSub.get(e.getKey())))
                .sorted(Comparator.comparing(DailySummaryDTO.ProductSummaryItem::getSubtotal).reversed())
                .toList();

        return new DailySummaryDTO(
                LocalDate.now(),
                shift.getStore().getId(),
                shift.getStore().getName(),
                openSales.size(),
                totalSubtotal,
                totalIsv,
                totalAmount,
                summary
        );
    }

    // ── Cierre de turno ───────────────────────────────────────────────────────

    @Transactional
    public DailyClosingResponseDTO closeShift(Long shiftId, String username) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));

        if ("CLOSED".equals(shift.getStatus())) {
            throw new IllegalStateException("El turno ya está cerrado");
        }

        List<Sale> openSales = saleRepository.findOpenByShiftId(shiftId);

        if (openSales.isEmpty()) {
            throw new IllegalStateException("No hay ventas abiertas para cerrar en este turno");
        }

        BigDecimal totalAmount = openSales.stream()
                .map(Sale::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Crear ClosingDeposit en sistema V1
        ClosingDeposit deposit = new ClosingDeposit();
        deposit.setAmount(totalAmount);
        deposit.setClosingsCount(openSales.size());
        deposit.setDepositDate(LocalDate.now());
        deposit.setPeriodStart(LocalDate.now());
        deposit.setPeriodEnd(LocalDate.now());
        deposit.setUsername(username);
        deposit.setStore(shift.getStore());
        ClosingDeposit saved = formsService.saveClosingDeposit(deposit);

        // Marcar ventas como CONFIRMED
        openSales.forEach(sale -> {
            sale.setStatus("CONFIRMED");
            saleRepository.save(sale);
        });

        // Cerrar el turno
        shift.setStatus("CLOSED");
        shiftRepository.save(shift);

        return new DailyClosingResponseDTO(
                shift.getId(),
                shift.getCode(),
                LocalDate.now(),
                shift.getStore().getId(),
                shift.getStore().getName(),
                openSales.size(),
                totalAmount,
                saved.getId()
        );
    }

    // ── Stock helpers ─────────────────────────────────────────────────────────

    private void deductStock(Long storeId, List<SaleItem> items) {
        for (SaleItem item : items) {
            if (item.getProduct() == null) continue;
            try {
                StockAdjustmentDTO adj = new StockAdjustmentDTO();
                adj.setProductId(item.getProduct().getId());
                adj.setType("SALIDA");
                adj.setQuantity(item.getQuantity());
                adj.setReason("Venta");
                adj.setUsername("system");
                inventoryService.adjustSilent(storeId, adj);
            } catch (Exception ignored) {
                // Stock insuficiente no bloquea la venta
            }
        }
    }

    private void revertStock(Long storeId, List<SaleItem> items) {
        for (SaleItem item : items) {
            if (item.getProduct() == null) continue;
            try {
                StockAdjustmentDTO adj = new StockAdjustmentDTO();
                adj.setProductId(item.getProduct().getId());
                adj.setType("ENTRADA");
                adj.setQuantity(item.getQuantity());
                adj.setReason("Cancelación de venta");
                adj.setUsername("system");
                inventoryService.adjustSilent(storeId, adj);
            } catch (Exception ignored) {}
        }
    }
}
