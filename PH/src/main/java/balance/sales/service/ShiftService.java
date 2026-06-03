package balance.sales.service;

import balance.model.Store;
import balance.repository.StoreRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class ShiftService {

    @Autowired private ShiftRepository shiftRepository;
    @Autowired private StoreRepository storeRepository;

    @Transactional
    public ShiftResponseDTO openShift(Long storeId, String username, BigDecimal openingCashAmount) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Local no encontrado"));

        if (shiftRepository.existsByStoreIdAndStatus(storeId, "OPEN")) {
            throw new IllegalStateException("Ya existe un turno abierto para este local");
        }

        Shift shift = new Shift();
        shift.setStore(store);
        shift.setUsername(username);
        shift.setStatus("OPEN");
        shift.setCode(generateCode(store));
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
        shift.setClosedAt(LocalDateTime.now());
        shiftRepository.save(shift);
        return ShiftResponseDTO.from(shift);
    }

    public ShiftResponseDTO getActiveShift(Long storeId) {
        return shiftRepository.findByStoreIdAndStatus(storeId, "OPEN")
                .map(ShiftResponseDTO::from)
                .orElse(null);
    }

    public List<ShiftResponseDTO> getShiftHistory(Long storeId, String username,
                                                    LocalDate from, LocalDate to,
                                                    int page, int size) {
        var pageable  = PageRequest.of(page, size);
        boolean hasUsername = username != null && !username.isBlank();
        boolean hasDates    = from != null || to != null;

        // Usar @Query solo cuando hay filtros de fecha (evita problemas con null params en Hibernate 6)
        if (hasDates) {
            var fromDt    = from != null ? from.atStartOfDay()      : null;
            var toDt      = to   != null ? to.atTime(LocalTime.MAX) : null;
            var usernameP = hasUsername ? username : null;
            return shiftRepository.findByFilters(storeId, usernameP, fromDt, toDt, pageable)
                    .stream().map(ShiftResponseDTO::from).toList();
        }

        // Sin fechas: queries derivadas simples (mas estables en Hibernate 6)
        if (hasUsername) {
            return shiftRepository.findByStoreIdAndUsernameOrderByOpenedAtDesc(storeId, username, pageable)
                    .stream().map(ShiftResponseDTO::from).toList();
        }

        return shiftRepository.findByStoreIdOrderByOpenedAtDesc(storeId, pageable)
                .stream().map(ShiftResponseDTO::from).toList();
    }

    public ShiftResponseDTO getById(Long shiftId) {
        return shiftRepository.findById(shiftId)
                .map(ShiftResponseDTO::from)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));
    }

    /** Genera código único de turno: T-YYYYMMDD-HHmmss-DAN
     *  Incluye segundos para garantizar unicidad incluso con turnos consecutivos. */
    private String generateCode(Store store) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        String storePart = store.getName()
                .replaceAll("[^a-zA-Z]", "")
                .toUpperCase();
        if (storePart.length() > 3) storePart = storePart.substring(0, 3);
        return "T-" + date + "-" + time + "-" + storePart;
    }
}
