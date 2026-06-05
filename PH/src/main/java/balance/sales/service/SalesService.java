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
import balance.sales.dto.ShiftExpenseRequestDTO;
import balance.sales.dto.ShiftExpenseResponseDTO;
import balance.sales.model.ShiftExpense;
import balance.sales.repository.SaleRepository;
import balance.sales.repository.ShiftExpenseRepository;
import balance.sales.repository.ShiftRepository;
import balance.service.FormsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;



@Service
public class SalesService {

    private static final Logger log = LoggerFactory.getLogger(SalesService.class);
    private static final ZoneId HONDURAS_TZ = ZoneId.of("America/Tegucigalpa");

    // ISV deshabilitado por solicitud del cliente (no cobra impuesto desglosado)
    private static final BigDecimal ISV_RATE = BigDecimal.ZERO;

    @Autowired private SaleRepository saleRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private ShiftExpenseRepository shiftExpenseRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private InventoryService inventoryService;
    @Autowired private FormsService formsService;

    // ── Crear venta ──────────────────────────────────────────────────────────

    /**
     * Registra una venta dentro de un turno abierto.
     * Calcula ISV 15%, guarda snapshot de precio de cada ítem y descuenta stock.
     * @throws IllegalArgumentException si el turno o algún producto no existe
     * @throws IllegalStateException    si el turno ya está cerrado
     */
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
        sale.setSaleDate(LocalDate.now(HONDURAS_TZ));
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

        // Método de pago
        String paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH";
        sale.setPaymentMethod(paymentMethod);

        switch (paymentMethod) {
            case "CARD":
                sale.setCashAmount(BigDecimal.ZERO);
                sale.setCardAmount(total);
                break;
            case "MIXED":
                BigDecimal cash = request.getCashAmount() != null ? request.getCashAmount() : BigDecimal.ZERO;
                BigDecimal card = request.getCardAmount() != null ? request.getCardAmount() : BigDecimal.ZERO;
                if (cash.add(card).compareTo(total) != 0) {
                    throw new IllegalArgumentException(
                        "En pago mixto, efectivo + tarjeta debe ser igual al total (" + total + ")");
                }
                sale.setCashAmount(cash);
                sale.setCardAmount(card);
                break;
            default: // CASH
                sale.setCashAmount(total);
                sale.setCardAmount(BigDecimal.ZERO);
                break;
        }

        saleRepository.save(sale);

        // Descontar stock de cada producto vendido
        deductStock(store.getId(), sale.getItems());

        return SaleResponseDTO.from(sale);
    }

    // ── Cancelar venta ───────────────────────────────────────────────────────

    /**
     * Cancela una venta con status OPEN y revierte el stock descontado.
     * No se puede cancelar una venta ya CONFIRMED (incluida en cierre).
     */
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

    // ── Ventas por local (admin) ───────────────────────────────────────────────

    public List<SaleResponseDTO> getSalesByStore(Long storeId, LocalDate from, LocalDate to) {
        return saleRepository.findByStoreIdAndDateRange(storeId, from, to)
                .stream().map(SaleResponseDTO::from).toList();
    }

    public DailySummaryDTO getSummaryByStore(Long storeId, LocalDate from, LocalDate to) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Local no encontrado"));

        List<Sale> sales = saleRepository.findByStoreIdAndDateRange(storeId, from, to);

        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalIsv      = BigDecimal.ZERO;
        BigDecimal totalAmount   = BigDecimal.ZERO;
        BigDecimal totalCash     = BigDecimal.ZERO;
        BigDecimal totalCard     = BigDecimal.ZERO;

        Map<String, int[]>        productQty = new LinkedHashMap<>();
        Map<String, BigDecimal>   productSub = new LinkedHashMap<>();
        Map<String, Long>         productIds = new LinkedHashMap<>();

        for (Sale sale : sales) {
            totalSubtotal = totalSubtotal.add(sale.getSubtotal());
            totalIsv      = totalIsv.add(sale.getIsv());
            totalAmount   = totalAmount.add(sale.getTotal());
            totalCash     = totalCash.add(sale.getCashAmount() != null ? sale.getCashAmount() : BigDecimal.ZERO);
            totalCard     = totalCard.add(sale.getCardAmount() != null ? sale.getCardAmount() : BigDecimal.ZERO);
            for (SaleItem item : sale.getItems()) {
                String key = item.getProductNameSnapshot();
                productQty.merge(key, new int[]{item.getQuantity()}, (a, b) -> new int[]{a[0] + b[0]});
                productSub.merge(key, item.getSubtotal(), BigDecimal::add);
                if (item.getProduct() != null) productIds.putIfAbsent(key, item.getProduct().getId());
            }
        }

        List<DailySummaryDTO.ProductSummaryItem> summary = productQty.entrySet().stream()
                .map(e -> new DailySummaryDTO.ProductSummaryItem(
                        productIds.get(e.getKey()), e.getKey(),
                        e.getValue()[0], productSub.get(e.getKey())))
                .sorted(Comparator.comparing(DailySummaryDTO.ProductSummaryItem::getSubtotal).reversed())
                .toList();

        LocalDate rangeDate = from != null ? from : LocalDate.now();
        return new DailySummaryDTO(rangeDate, store.getId(), store.getName(),
                sales.size(), totalSubtotal, totalIsv, totalAmount,
                BigDecimal.ZERO, totalCash, totalCard, BigDecimal.ZERO, summary);
    }

    // ── Resumen diario ────────────────────────────────────────────────────────

    public DailySummaryDTO getDailySummary(Long shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));

        // Muestra todas las ventas del turno (OPEN o CONFIRMED) para permitir
        // consultar el resumen incluso después del cierre
        List<Sale> openSales = saleRepository.findByShiftIdOrderByCreatedAtDesc(shiftId);

        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalIsv      = BigDecimal.ZERO;
        BigDecimal totalAmount   = BigDecimal.ZERO;
        BigDecimal totalCash     = BigDecimal.ZERO;
        BigDecimal totalCard     = BigDecimal.ZERO;

        Map<String, int[]> productQty       = new LinkedHashMap<>();
        Map<String, BigDecimal> productSub  = new LinkedHashMap<>();
        Map<String, Long> productIds        = new LinkedHashMap<>();

        for (Sale sale : openSales) {
            totalSubtotal = totalSubtotal.add(sale.getSubtotal());
            totalIsv      = totalIsv.add(sale.getIsv());
            totalAmount   = totalAmount.add(sale.getTotal());
            totalCash     = totalCash.add(sale.getCashAmount() != null ? sale.getCashAmount() : BigDecimal.ZERO);
            totalCard     = totalCard.add(sale.getCardAmount() != null ? sale.getCardAmount() : BigDecimal.ZERO);

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

        BigDecimal openingCash    = shift.getOpeningCashAmount() != null ? shift.getOpeningCashAmount() : BigDecimal.ZERO;
        BigDecimal shiftExpenses  = shiftExpenseRepository.sumAmountByShiftId(shiftId);

        return new DailySummaryDTO(
                LocalDate.now(),
                shift.getStore().getId(),
                shift.getStore().getName(),
                openSales.size(),
                totalSubtotal,
                totalIsv,
                totalAmount,
                openingCash,
                totalCash,
                totalCard,
                shiftExpenses,
                summary
        );
    }

    // ── Egresos del turno ─────────────────────────────────────────────────────

    @Transactional
    public ShiftExpenseResponseDTO addExpense(Long shiftId, ShiftExpenseRequestDTO request) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));

        if ("CLOSED".equals(shift.getStatus())) {
            throw new IllegalStateException("No se pueden agregar egresos a un turno cerrado");
        }

        ShiftExpense expense = new ShiftExpense();
        expense.setShift(shift);
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setUsername(request.getUsername() != null ? request.getUsername() : "unknown");

        shiftExpenseRepository.save(expense);
        return ShiftExpenseResponseDTO.from(expense);
    }

    public List<ShiftExpenseResponseDTO> getExpenses(Long shiftId) {
        return shiftExpenseRepository.findByShiftIdOrderByCreatedAtAsc(shiftId)
                .stream().map(ShiftExpenseResponseDTO::from).toList();
    }

    // ── Resumen de efectivo para reconciliación bancaria ─────────────────────

    public Map<String, Object> getCashSummary(Long storeId, LocalDate from, LocalDate to) {
        storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Local no encontrado"));

        List<Sale> sales = saleRepository.findByStoreIdAndDateRangeStrict(storeId, from, to);

        BigDecimal totalCash  = sales.stream().map(Sale::getCashAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCard  = sales.stream().map(Sale::getCardAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSales = sales.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "storeId",     storeId,
                "from",        from.toString(),
                "to",          to.toString(),
                "totalSales",  totalSales,
                "totalCash",   totalCash,
                "totalCard",   totalCard,
                "saleCount",   sales.size()
        );
    }

    // ── Cierre de turno ───────────────────────────────────────────────────────

    /**
     * Cierra el turno: confirma todas las ventas OPEN, crea un ClosingDeposit
     * en el sistema financiero V1 y registra la hora de cierre.
     * @throws IllegalStateException si el turno ya está cerrado o no hay ventas
     */
    @Transactional
    public DailyClosingResponseDTO closeShift(Long shiftId, String username, BigDecimal declaredCashAmount) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));

        if ("CLOSED".equals(shift.getStatus())) {
            throw new IllegalStateException("El turno ya está cerrado");
        }

        List<Sale> openSales = saleRepository.findOpenByShiftId(shiftId);

        if (openSales.isEmpty()) {
            throw new IllegalStateException("No hay ventas abiertas para cerrar en este turno");
        }

        // Calcular totales por método de pago
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalCash   = BigDecimal.ZERO;
        BigDecimal totalCard   = BigDecimal.ZERO;

        for (Sale sale : openSales) {
            totalAmount = totalAmount.add(sale.getTotal());
            totalCash   = totalCash.add(sale.getCashAmount()  != null ? sale.getCashAmount()  : BigDecimal.ZERO);
            totalCard   = totalCard.add(sale.getCardAmount() != null ? sale.getCardAmount() : BigDecimal.ZERO);
        }

        // Reconciliación: efectivo esperado = fondo inicial + ventas en efectivo - egresos del turno
        BigDecimal opening    = shift.getOpeningCashAmount() != null ? shift.getOpeningCashAmount() : BigDecimal.ZERO;
        BigDecimal declared   = declaredCashAmount != null ? declaredCashAmount : BigDecimal.ZERO;
        BigDecimal expensesRaw = shiftExpenseRepository.sumAmountByShiftId(shiftId);
        BigDecimal expenses    = expensesRaw != null ? expensesRaw : BigDecimal.ZERO;
        BigDecimal expected    = opening.add(totalCash).subtract(expenses);
        BigDecimal difference = declared.subtract(expected).setScale(2, RoundingMode.HALF_UP);

        // Crear ClosingDeposit en sistema V1 vinculado a este turno
        ClosingDeposit deposit = new ClosingDeposit();
        deposit.setAmount(totalAmount);
        deposit.setClosingsCount(openSales.size());
        deposit.setDepositDate(LocalDate.now());
        deposit.setPeriodStart(LocalDate.now());
        deposit.setPeriodEnd(LocalDate.now());
        deposit.setUsername(username);
        deposit.setStore(shift.getStore());
        deposit.setShiftId(shiftId);
        ClosingDeposit saved = formsService.saveClosingDeposit(deposit);

        // Marcar ventas como CONFIRMED
        openSales.forEach(sale -> {
            sale.setStatus("CONFIRMED");
            saleRepository.save(sale);
        });

        // Cerrar el turno con todos los datos de reconciliación
        shift.setStatus("CLOSED");
        shift.setClosedAt(java.time.LocalDateTime.now());
        shift.setTotalCashSales(totalCash);
        shift.setTotalCardSales(totalCard);
        shift.setTotalShiftExpenses(expenses);
        shift.setDeclaredCashAmount(declared);
        shift.setCashDifference(difference);
        shiftRepository.save(shift);

        return new DailyClosingResponseDTO(
                shift.getId(),
                shift.getCode(),
                LocalDate.now(),
                shift.getStore().getId(),
                shift.getStore().getName(),
                openSales.size(),
                totalAmount,
                opening,
                totalCash,
                expenses,
                totalCard,
                declared,
                difference,
                saved.getId()
        );
    }

    // ── Stock helpers ─────────────────────────────────────────────────────────

    private void deductStock(Long storeId, List<SaleItem> items) {
        for (SaleItem item : items) {
            if (item.getProduct() == null) {
                log.warn("SaleItem sin producto asociado — se omite descuento de stock");
                continue;
            }
            StockAdjustmentDTO adj = new StockAdjustmentDTO();
            adj.setProductId(item.getProduct().getId());
            adj.setType("SALIDA");
            adj.setQuantity(item.getQuantity());
            adj.setReason("Venta");
            adj.setUsername("system");
            inventoryService.adjustSilent(storeId, adj); // logea si stock insuficiente
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
