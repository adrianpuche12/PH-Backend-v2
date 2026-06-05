package balance.deposit.service;

import balance.deposit.dto.CreateDepositRequest;
import balance.deposit.dto.DepositResponse;
import balance.deposit.dto.PendingClosingResponse;
import balance.deposit.model.BankDeposit;
import balance.deposit.repository.BankDepositRepository;
import balance.model.ClosingDeposit;
import balance.model.Store;
import balance.repository.ClosingDepositRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankDepositServiceTest {

    @Mock private BankDepositRepository depositRepo;
    @Mock private ClosingDepositRepository closingDepositRepo;
    @InjectMocks private BankDepositService service;

    // ── getPendingClosings ────────────────────────────────────────────────────

    @Test
    void getPendingClosings_withDateRange_usesDateFilterQuery() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to   = LocalDate.of(2026, 6, 5);
        when(closingDepositRepo
            .findByStoreIdAndDepositStatusAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualOrderByDepositDateDesc(
                1L, "PENDING", from, to))
            .thenReturn(List.of());

        List<PendingClosingResponse> result = service.getPendingClosings(1L, from, to);

        assertThat(result).isEmpty();
        verify(closingDepositRepo)
            .findByStoreIdAndDepositStatusAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualOrderByDepositDateDesc(
                1L, "PENDING", from, to);
        verifyNoMoreInteractions(closingDepositRepo);
    }

    @Test
    void getPendingClosings_withoutDateRange_usesSimpleQuery() {
        when(closingDepositRepo.findByStoreIdAndDepositStatusOrderByDepositDateDesc(1L, "PENDING"))
            .thenReturn(List.of());

        service.getPendingClosings(1L, null, null);

        verify(closingDepositRepo).findByStoreIdAndDepositStatusOrderByDepositDateDesc(1L, "PENDING");
    }

    @Test
    void getPendingClosings_mapsClosingsToResponse() {
        ClosingDeposit closing = buildClosing(10L, new BigDecimal("500.00"), "PENDING");
        when(closingDepositRepo.findByStoreIdAndDepositStatusOrderByDepositDateDesc(1L, "PENDING"))
            .thenReturn(List.of(closing));

        List<PendingClosingResponse> result = service.getPendingClosings(1L, null, null);

        assertThat(result).hasSize(1);
    }

    // ── createDeposit ─────────────────────────────────────────────────────────

    @Test
    void createDeposit_calculatesExpectedCashAsSumOfClosings() {
        ClosingDeposit c1 = buildClosing(1L, new BigDecimal("300.00"), "PENDING");
        ClosingDeposit c2 = buildClosing(2L, new BigDecimal("250.00"), "PENDING");
        when(closingDepositRepo.findAllById(List.of(1L, 2L))).thenReturn(List.of(c1, c2));
        BankDeposit saved = buildBankDeposit(99L, new BigDecimal("550.00"), new BigDecimal("550.00"), BigDecimal.ZERO, "CONFIRMED");
        when(depositRepo.save(any())).thenReturn(saved);

        CreateDepositRequest req = buildRequest(List.of(1L, 2L), new BigDecimal("550.00"));
        service.createDeposit("cajero01", req);

        ArgumentCaptor<BankDeposit> captor = ArgumentCaptor.forClass(BankDeposit.class);
        verify(depositRepo).save(captor.capture());
        assertThat(captor.getValue().getExpectedCash()).isEqualByComparingTo("550.00");
    }

    @Test
    void createDeposit_statusIsConfirmedWhenNoDifference() {
        ClosingDeposit c = buildClosing(1L, new BigDecimal("400.00"), "PENDING");
        when(closingDepositRepo.findAllById(List.of(1L))).thenReturn(List.of(c));
        BankDeposit saved = buildBankDeposit(1L, new BigDecimal("400.00"), new BigDecimal("400.00"), BigDecimal.ZERO, "CONFIRMED");
        when(depositRepo.save(any())).thenReturn(saved);

        service.createDeposit("cajero01", buildRequest(List.of(1L), new BigDecimal("400.00")));

        ArgumentCaptor<BankDeposit> captor = ArgumentCaptor.forClass(BankDeposit.class);
        verify(depositRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void createDeposit_statusIsDiscrepancyWhenAmountDiffers() {
        ClosingDeposit c = buildClosing(1L, new BigDecimal("400.00"), "PENDING");
        when(closingDepositRepo.findAllById(List.of(1L))).thenReturn(List.of(c));
        BankDeposit saved = buildBankDeposit(1L, new BigDecimal("400.00"), new BigDecimal("350.00"), new BigDecimal("-50.00"), "DISCREPANCY");
        when(depositRepo.save(any())).thenReturn(saved);

        service.createDeposit("cajero01", buildRequest(List.of(1L), new BigDecimal("350.00")));

        ArgumentCaptor<BankDeposit> captor = ArgumentCaptor.forClass(BankDeposit.class);
        verify(depositRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("DISCREPANCY");
    }

    @Test
    void createDeposit_marksClosingsAsDeposited() {
        ClosingDeposit c1 = buildClosing(1L, new BigDecimal("300.00"), "PENDING");
        ClosingDeposit c2 = buildClosing(2L, new BigDecimal("200.00"), "PENDING");
        when(closingDepositRepo.findAllById(List.of(1L, 2L))).thenReturn(List.of(c1, c2));
        BankDeposit saved = buildBankDeposit(99L, new BigDecimal("500.00"), new BigDecimal("500.00"), BigDecimal.ZERO, "CONFIRMED");
        when(depositRepo.save(any())).thenReturn(saved);

        service.createDeposit("cajero01", buildRequest(List.of(1L, 2L), new BigDecimal("500.00")));

        assertThat(c1.getDepositStatus()).isEqualTo("DEPOSITED");
        assertThat(c2.getDepositStatus()).isEqualTo("DEPOSITED");
        assertThat(c1.getBankDepositId()).isEqualTo(99L);
        verify(closingDepositRepo).saveAll(List.of(c1, c2));
    }

    @Test
    void createDeposit_throwsWhenClosingNotFound() {
        when(closingDepositRepo.findAllById(List.of(1L, 2L))).thenReturn(List.of(buildClosing(1L, BigDecimal.TEN, "PENDING")));

        assertThatThrownBy(() -> service.createDeposit("cajero01", buildRequest(List.of(1L, 2L), BigDecimal.TEN)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no existen");
    }

    @Test
    void createDeposit_throwsWhenClosingAlreadyDeposited() {
        ClosingDeposit deposited = buildClosing(1L, new BigDecimal("300.00"), "DEPOSITED");
        when(closingDepositRepo.findAllById(List.of(1L))).thenReturn(List.of(deposited));

        assertThatThrownBy(() -> service.createDeposit("cajero01", buildRequest(List.of(1L), new BigDecimal("300.00"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ya fue depositado");
    }

    @Test
    void createDeposit_storesCreatedByUsername() {
        ClosingDeposit c = buildClosing(1L, new BigDecimal("100.00"), "PENDING");
        when(closingDepositRepo.findAllById(List.of(1L))).thenReturn(List.of(c));
        BankDeposit saved = buildBankDeposit(1L, new BigDecimal("100.00"), new BigDecimal("100.00"), BigDecimal.ZERO, "CONFIRMED");
        when(depositRepo.save(any())).thenReturn(saved);

        service.createDeposit("ana123", buildRequest(List.of(1L), new BigDecimal("100.00")));

        ArgumentCaptor<BankDeposit> captor = ArgumentCaptor.forClass(BankDeposit.class);
        verify(depositRepo).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("ana123");
    }

    // ── confirmDeposit ────────────────────────────────────────────────────────

    @Test
    void confirmDeposit_setsStatusToConfirmed() {
        BankDeposit deposit = buildBankDeposit(1L, new BigDecimal("500.00"), new BigDecimal("450.00"), new BigDecimal("-50.00"), "DISCREPANCY");
        when(depositRepo.findById(1L)).thenReturn(java.util.Optional.of(deposit));
        when(depositRepo.save(any())).thenReturn(deposit);

        service.confirmDeposit(1L);

        assertThat(deposit.getStatus()).isEqualTo("CONFIRMED");
        verify(depositRepo).save(deposit);
    }

    @Test
    void confirmDeposit_throwsWhenNotFound() {
        when(depositRepo.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.confirmDeposit(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no encontrado");
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_throwsWhenNotFound() {
        when(depositRepo.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no encontrado");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ClosingDeposit buildClosing(Long id, BigDecimal amount, String status) {
        Store store = new Store();
        store.setName("Test Store");
        org.springframework.test.util.ReflectionTestUtils.setField(store, "id", 1L);

        ClosingDeposit c = new ClosingDeposit();
        org.springframework.test.util.ReflectionTestUtils.setField(c, "id", id);
        c.setAmount(amount);
        c.setDepositStatus(status);
        c.setDepositDate(LocalDate.now());
        c.setStore(store);
        return c;
    }

    private BankDeposit buildBankDeposit(Long id, BigDecimal expected, BigDecimal declared, BigDecimal diff, String status) {
        BankDeposit d = new BankDeposit();
        org.springframework.test.util.ReflectionTestUtils.setField(d, "id", id);
        d.setExpectedCash(expected);
        d.setDeclaredAmount(declared);
        d.setDifference(diff);
        d.setStatus(status);
        d.setStoreIds("1");
        d.setShiftIds(id.toString());
        d.setDepositDate(LocalDate.now());
        d.setCreatedBy("test");
        return d;
    }

    private CreateDepositRequest buildRequest(List<Long> ids, BigDecimal declared) {
        CreateDepositRequest req = new CreateDepositRequest();
        req.setShiftIds(ids);
        req.setDeclaredAmount(declared);
        req.setDepositDate(LocalDate.now());
        return req;
    }
}
