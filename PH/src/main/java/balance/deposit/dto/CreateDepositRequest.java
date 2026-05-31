package balance.deposit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CreateDepositRequest {

    @NotEmpty
    private List<Long> shiftIds;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate depositDate;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal declaredAmount;

    private String imageUri;

    private String notes;

    public List<Long> getShiftIds() { return shiftIds; }
    public void setShiftIds(List<Long> shiftIds) { this.shiftIds = shiftIds; }

    public LocalDate getDepositDate() { return depositDate; }
    public void setDepositDate(LocalDate depositDate) { this.depositDate = depositDate; }

    public BigDecimal getDeclaredAmount() { return declaredAmount; }
    public void setDeclaredAmount(BigDecimal declaredAmount) { this.declaredAmount = declaredAmount; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
