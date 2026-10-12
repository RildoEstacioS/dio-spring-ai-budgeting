package dio.budgeting.application;

import dio.budgeting.application.input.DashboardQuery;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.PeriodPreset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static dio.budgeting.domain.TestTransactions.CLOCK;
import static dio.budgeting.domain.TestTransactions.TODAY;
import static dio.budgeting.domain.TestTransactions.entry;
import static dio.budgeting.domain.TestTransactions.violatedFields;
import static org.assertj.core.api.Assertions.assertThat;

class GetDashboardSummaryUseCaseTest {
    private InMemoryTransactionRepository transactionRepository;
    private GetDashboardSummaryUseCase useCase;

    @BeforeEach
    void setUp() {
        transactionRepository = new InMemoryTransactionRepository();
        useCase = new GetDashboardSummaryUseCase(transactionRepository, CLOCK);
    }

    @Test
    void should_useThisMonth_when_noPeriodIsGiven() {
        var summary = useCase.execute(new DashboardQuery(null, null, null));

        assertThat(summary.preset()).isEqualTo("THIS_MONTH");
        assertThat(summary.period().start()).isEqualTo(LocalDate.parse("2026-10-01"));
    }

    @Test
    void should_countOverduePlannedEntries_fromAnyPeriod() {
        transactionRepository.save(entry().on(TODAY.minusMonths(3)).planned().build());
        transactionRepository.save(entry().on(TODAY).planned().build());
        transactionRepository.save(entry().expense("Mercado", 4500, Category.GROCERIES).build());

        var summary = useCase.execute(new DashboardQuery(PeriodPreset.THIS_MONTH, null, null));

        assertThat(summary.forecast().overduePlannedCount()).isEqualTo(1);
        assertThat(summary.expense().totalInCents()).isEqualTo(4500);
    }

    @Test
    void should_rejectAmbiguousOrIncompletePeriods() {
        assertThat(violatedFields(() -> useCase.execute(
                new DashboardQuery(PeriodPreset.THIS_MONTH, LocalDate.parse("2026-10-01"), null)))).containsExactly("period");
        assertThat(violatedFields(() -> useCase.execute(
                new DashboardQuery(null, LocalDate.parse("2026-10-01"), null)))).containsExactly("end");
        assertThat(violatedFields(() -> useCase.execute(
                new DashboardQuery(null, LocalDate.parse("2025-01-01"), LocalDate.parse("2026-10-01"))))).containsExactly("end");
    }

    @Test
    void should_acceptCustomRange_when_within366Days() {
        var summary = useCase.execute(new DashboardQuery(null, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-10")));

        assertThat(summary.preset()).isNull();
        assertThat(summary.previousPeriod().start()).isEqualTo(LocalDate.parse("2026-09-21"));
    }
}
