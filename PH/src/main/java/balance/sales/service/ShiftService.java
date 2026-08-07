package balance.sales.service;

import balance.model.Store;
import balance.repository.StoreRepository;
import balance.sales.dto.ShiftEditDTO;
import balance.sales.dto.ShiftResponseDTO;
import java.math.BigDecimal;
import balance.sales.model.Shift;
import balance.sales.repository.ShiftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class ShiftService {

    private static final ZoneId HONDURAS_TZ = ZoneId.of("America/Tegucigalpa");

    @Autowired private ShiftRepository shiftRepository;
    @Autowired private StoreRepository storeRepository;

    @Transactional
    public ShiftResponseDTO openShift(Long storeId, String username, BigDecimal openingCashAmount) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Local no encontrado"));

        if (shiftRepository.existsByStoreIdAndUsernameAndStatus(storeId, username, "OPEN")) {
            throw new IllegalStateException("Ya tenés un turno abierto en este local");
        }

        Shift shift = new Shift();
        shift.setStore(store);
        shift.setUsername(username);
        shift.setStatus("OPEN");
        shift.setCode(generateCode(store));
        shift.setOpenedAt(LocalDateTime.now(HONDURAS_TZ));
        shift.setOpeningCashAmount(openingCashAmount != null ? openingCashAmount : BigDecimal.ZERO);
        shiftRepository.save(shift);
        return ShiftResponseDTO.from(shift);
    }

    @Transactional
    public ShiftResponseDTO closeShift(Long shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));
        if ("CLOSED".equals(shift.getStatus())) {
            throw new IllegalStateException("El turno ya está cerrado");
        }
        shift.setStatus("CLOSED");
        shift.setClosedAt(LocalDateTime.now(HONDURAS_TZ));
        shiftRepository.save(shift);
        return ShiftResponseDTO.from(shift);
    }

    public ShiftResponseDTO getActiveShift(Long storeId, String username) {
        return shiftRepository.findByStoreIdAndUsernameAndStatus(storeId, username, "OPEN")
                .map(ShiftResponseDTO::from)
                .orElse(null);
    }

    public List<ShiftResponseDTO> getShiftHistory(Long storeId, String username,
                                                    LocalDate from, LocalDate to,
                                                    int page, int size) {
        var pageable  = PageRequest.of(page, size);
        boolean hasUsername = username != null && !username.isBlank();
        boolean hasDates    = from != null || to != null;

        // Queries derivadas 100% Hibernate 6 compatible — sin null params en JPQL
        if (hasDates) {
            var fromDt = (from != null ? from : LocalDate.of(2000, 1, 1)).atStartOfDay();
            var toDt   = (to   != null ? to   : LocalDate.of(2099, 12, 31)).atTime(LocalTime.MAX);
            if (hasUsername) {
                return shiftRepository
                        .findByStoreIdAndUsernameAndOpenedAtBetweenOrderByOpenedAtDesc(storeId, username, fromDt, toDt, pageable)
                        .stream().map(ShiftResponseDTO::from).toList();
            }
            return shiftRepository
                    .findByStoreIdAndOpenedAtBetweenOrderByOpenedAtDesc(storeId, fromDt, toDt, pageable)
                    .stream().map(ShiftResponseDTO::from).toList();
        }

        if (hasUsername) {
            return shiftRepository.findByStoreIdAndUsernameOrderByOpenedAtDesc(storeId, username, pageable)
                    .stream().map(ShiftResponseDTO::from).toList();
        }

        return shiftRepository.findByStoreIdOrderByOpenedAtDesc(storeId, pageable)
                .stream().map(ShiftResponseDTO::from).toList();
    }

    public List<ShiftResponseDTO> getShiftsByUsername(String username, int page, int size) {
        var pageable = PageRequest.of(page, size);
        return shiftRepository.findByUsernameOrderByOpenedAtDesc(username, pageable)
                .stream().map(ShiftResponseDTO::from).toList();
    }

    public ShiftResponseDTO getById(Long shiftId) {
        return shiftRepository.findById(shiftId)
                .map(ShiftResponseDTO::from)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));
    }

    @Transactional
    public ShiftResponseDTO editShift(Long shiftId, ShiftEditDTO dto) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));

        if (dto.getUsername()           != null) shift.setUsername(dto.getUsername().trim());
        if (dto.getOpenedAt()           != null) shift.setOpenedAt(dto.getOpenedAt());
        if (dto.getClosedAt()           != null) shift.setClosedAt(dto.getClosedAt());
        if (dto.getOpeningCashAmount()  != null) shift.setOpeningCashAmount(dto.getOpeningCashAmount());
        if (dto.getTotalCashSales()     != null) shift.setTotalCashSales(dto.getTotalCashSales());
        if (dto.getTotalCardSales()     != null) shift.setTotalCardSales(dto.getTotalCardSales());
        if (dto.getTotalShiftExpenses() != null) shift.setTotalShiftExpenses(dto.getTotalShiftExpenses());
        if (dto.getDeclaredCashAmount() != null) shift.setDeclaredCashAmount(dto.getDeclaredCashAmount());
        shift.setNotes(dto.getNotes()); // permite borrar notas enviando null

        // Recalcular diferencia si hay datos suficientes
        BigDecimal declared = shift.getDeclaredCashAmount();
        if (declared != null) {
            BigDecimal cash     = orZero(shift.getTotalCashSales());
            BigDecimal expenses = orZero(shift.getTotalShiftExpenses());
            shift.setCashDifference(declared.subtract(cash.subtract(expenses)));
        }

        shiftRepository.save(shift);
        return ShiftResponseDTO.from(shift);
    }

    private BigDecimal orZero(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    /** Genera código único de turno: T-YYYYMMDD-HHmmss-DAN
     *  Incluye segundos para garantizar unicidad incluso con turnos consecutivos. */
    private String generateCode(Store store) {
        String date = LocalDate.now(HONDURAS_TZ).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = LocalTime.now(HONDURAS_TZ).format(DateTimeFormatter.ofPattern("HHmmss"));
        String storePart = store.getName()
                .replaceAll("[^a-zA-Z]", "")
                .toUpperCase();
        if (storePart.length() > 3) storePart = storePart.substring(0, 3);
        return "T-" + date + "-" + time + "-" + storePart;
    }
}
