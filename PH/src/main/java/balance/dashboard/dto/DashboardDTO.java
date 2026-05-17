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

    public DashboardDTO(List<StoreDashboardDTO> stores, long totalSalesToday, BigDecimal totalAmountToday) {
        this.stores              = stores;
        this.totalActiveShifts   = stores.stream().filter(StoreDashboardDTO::isHasActiveShift).count();
        this.totalSalesToday     = totalSalesToday;
        this.totalAmountToday    = totalAmountToday != null ? totalAmountToday : BigDecimal.ZERO;
        this.totalLowStockAlerts = stores.stream().mapToLong(StoreDashboardDTO::getLowStockCount).sum();
    }

    public List<StoreDashboardDTO> getStores()       { return stores; }
    public long getTotalActiveShifts()               { return totalActiveShifts; }
    public long getTotalSalesToday()                 { return totalSalesToday; }
    public BigDecimal getTotalAmountToday()          { return totalAmountToday; }
    public long getTotalLowStockAlerts()             { return totalLowStockAlerts; }
}
