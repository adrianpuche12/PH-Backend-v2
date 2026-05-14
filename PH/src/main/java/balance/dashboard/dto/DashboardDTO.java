package balance.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/** Respuesta completa del endpoint GET /api/v2/dashboard */
public class DashboardDTO {

    private List<StoreDashboardDTO> stores;
    private long       totalActiveShifts;
    private long       totalSalesToday;
    private BigDecimal totalAmountToday;
    private long       totalLowStockAlerts;

    public DashboardDTO(List<StoreDashboardDTO> stores) {
        this.stores             = stores;
        this.totalActiveShifts  = stores.stream().filter(StoreDashboardDTO::isHasActiveShift).count();
        this.totalSalesToday    = stores.stream().mapToLong(StoreDashboardDTO::getShiftSalesCount).sum();
        this.totalAmountToday   = stores.stream()
                .map(s -> s.getShiftSalesTotal() != null ? s.getShiftSalesTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalLowStockAlerts = stores.stream().mapToLong(StoreDashboardDTO::getLowStockCount).sum();
    }

    public List<StoreDashboardDTO> getStores()       { return stores; }
    public long getTotalActiveShifts()               { return totalActiveShifts; }
    public long getTotalSalesToday()                 { return totalSalesToday; }
    public BigDecimal getTotalAmountToday()          { return totalAmountToday; }
    public long getTotalLowStockAlerts()             { return totalLowStockAlerts; }
}
