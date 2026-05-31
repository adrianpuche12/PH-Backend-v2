package balance.deposit.dto;

import balance.deposit.model.BankDeposit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DepositResponse {

    private Long id;
    private String createdBy;
    private LocalDate depositDate;
    private List<Long> storeIds;
    private List<Long> shiftIds;
    private BigDecimal expectedCash;
    private BigDecimal declaredAmount;
    private BigDecimal difference;
    private String status;
    private String imageUri;
    private String notes;
    private LocalDateTime createdAt;

    public static DepositResponse from(BankDeposit d) {
        DepositResponse r = new DepositResponse();
        r.id             = d.getId();
        r.createdBy      = d.getCreatedBy();
        r.depositDate    = d.getDepositDate();
        r.storeIds       = parseIds(d.getStoreIds());
        r.shiftIds       = parseIds(d.getShiftIds());
        r.expectedCash   = d.getExpectedCash();
        r.declaredAmount = d.getDeclaredAmount();
        r.difference     = d.getDifference();
        r.status         = d.getStatus();
        r.imageUri       = d.getImageUri();
        r.notes          = d.getNotes();
        r.createdAt      = d.getCreatedAt();
        return r;
    }

    private static List<Long> parseIds(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    public Long getId() { return id; }
    public String getCreatedBy() { return createdBy; }
    public LocalDate getDepositDate() { return depositDate; }
    public List<Long> getStoreIds() { return storeIds; }
    public List<Long> getShiftIds() { return shiftIds; }
    public BigDecimal getExpectedCash() { return expectedCash; }
    public BigDecimal getDeclaredAmount() { return declaredAmount; }
    public BigDecimal getDifference() { return difference; }
    public String getStatus() { return status; }
    public String getImageUri() { return imageUri; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
