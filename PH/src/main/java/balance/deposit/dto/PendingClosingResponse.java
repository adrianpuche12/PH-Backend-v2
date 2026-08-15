package balance.deposit.dto;

import balance.model.ClosingDeposit;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PendingClosingResponse {

    private Long id;
    private String storeName;
    private Long storeId;
    private String username;
    private LocalDate depositDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer closingsCount;
    private BigDecimal amount;
    private String depositStatus;
    private String imageUri;

    public static PendingClosingResponse from(ClosingDeposit c) {
        PendingClosingResponse r = new PendingClosingResponse();
        r.id            = c.getId();
        r.storeName     = c.getStore() != null ? c.getStore().getName() : null;
        r.storeId       = c.getStore() != null ? c.getStore().getId() : null;
        r.username      = c.getUsername();
        r.depositDate   = c.getDepositDate();
        r.periodStart   = c.getPeriodStart();
        r.periodEnd     = c.getPeriodEnd();
        r.closingsCount = c.getClosingsCount();
        r.amount        = c.getAmount();
        r.depositStatus = c.getDepositStatus();
        r.imageUri      = c.getImageUri();
        return r;
    }

    public Long getId() { return id; }
    public String getStoreName() { return storeName; }
    public Long getStoreId() { return storeId; }
    public String getUsername() { return username; }
    public LocalDate getDepositDate() { return depositDate; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public Integer getClosingsCount() { return closingsCount; }
    public BigDecimal getAmount() { return amount; }
    public String getDepositStatus() { return depositStatus; }
    public String getImageUri() { return imageUri; }
}
