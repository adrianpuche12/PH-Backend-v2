package balance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GastoAdminRequestDTO {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String descripcion;

    @NotBlank(message = "El tipo es obligatorio")
    @Pattern(regexp = "income|expense", message = "El tipo debe ser 'income' o 'expense'")
    private String tipo;

    @NotNull(message = "Las distribuciones son obligatorias")
    @Size(min = 1, message = "Debe incluir al menos un local")
    @Valid
    private List<StoreDistribucion> distribuciones;

    // ── Clase interna: distribución por local ────────────────────────────────

    public static class StoreDistribucion {

        @NotNull(message = "El storeId es obligatorio")
        private Long storeId;

        @NotNull(message = "El porcentaje es obligatorio")
        @Min(value = 1, message = "El porcentaje debe ser al menos 1")
        @Max(value = 100, message = "El porcentaje no puede superar 100")
        private Integer porcentaje;

        public StoreDistribucion() {}

        public StoreDistribucion(Long storeId, Integer porcentaje) {
            this.storeId = storeId;
            this.porcentaje = porcentaje;
        }

        public Long getStoreId()              { return storeId; }
        public void setStoreId(Long storeId)  { this.storeId = storeId; }

        public Integer getPorcentaje()                 { return porcentaje; }
        public void setPorcentaje(Integer porcentaje)  { this.porcentaje = porcentaje; }
    }

    // ── Constructor vacío ────────────────────────────────────────────────────

    public GastoAdminRequestDTO() {}

    // ── Validación: los porcentajes deben sumar exactamente 100 ─────────────

    public boolean isValidPercentages() {
        if (distribuciones == null || distribuciones.isEmpty()) return false;
        int total = distribuciones.stream()
                .filter(d -> d.getPorcentaje() != null)
                .mapToInt(StoreDistribucion::getPorcentaje)
                .sum();
        return total == 100;
    }

    // ── Getters y Setters ────────────────────────────────────────────────────

    public LocalDate getFecha()                      { return fecha; }
    public void setFecha(LocalDate fecha)            { this.fecha = fecha; }

    public BigDecimal getMonto()                     { return monto; }
    public void setMonto(BigDecimal monto)           { this.monto = monto; }

    public String getDescripcion()                   { return descripcion; }
    public void setDescripcion(String descripcion)   { this.descripcion = descripcion; }

    public String getTipo()                          { return tipo; }
    public void setTipo(String tipo)                 { this.tipo = tipo; }

    public List<StoreDistribucion> getDistribuciones()                       { return distribuciones; }
    public void setDistribuciones(List<StoreDistribucion> distribuciones)    { this.distribuciones = distribuciones; }

    @Override
    public String toString() {
        return "GastoAdminRequestDTO{" +
                "fecha=" + fecha +
                ", monto=" + monto +
                ", descripcion='" + descripcion + '\'' +
                ", tipo='" + tipo + '\'' +
                ", distribuciones=" + distribuciones +
                '}';
    }
}
