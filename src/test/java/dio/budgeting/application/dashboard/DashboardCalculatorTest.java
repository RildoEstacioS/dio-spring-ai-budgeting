package dio.budgeting.application.dashboard;

import dio.budgeting.application.dashboard.DashboardOutputs.InsightOutput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.PeriodPreset;
import dio.budgeting.domain.Recurrence;
import dio.budgeting.domain.ReportingPeriod;
import dio.budgeting.domain.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static dio.budgeting.domain.TestTransactions.TODAY;
import static dio.budgeting.domain.TestTransactions.entry;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardCalculatorTest {
    private static final ReportingPeriod THIS_MONTH = ReportingPeriod.of(PeriodPreset.THIS_MONTH, TODAY);

    private static List<Transaction> sampleEntries() {
        return List.of(
                // outubro (período atual)
                entry().income("Salário", 650000, Category.SALARY).on(date("2026-10-05")).build(),
                entry().expense("Aluguel", 180000, Category.HOUSING).on(date("2026-10-10")).recurrence(Recurrence.MONTHLY).build(),
                entry().expense("Supermercado - compra do mês", 48790, Category.GROCERIES).on(date("2026-10-07")).build(),
                entry().expense("Feira livre", 6350, Category.GROCERIES).on(date("2026-10-11")).build(),
                entry().expense("Academia", 9990, Category.LEISURE).on(date("2026-10-03")).build(),
                entry().expense("Farmácia - vitaminas", 8999, Category.PHARMA).on(date("2026-10-02")).build(),
                entry().income("Freelance", 95000, Category.FREELANCE).on(date("2026-10-20")).planned().build(),
                entry().expense("Internet", 11990, Category.BILLS).on(date("2026-10-15")).planned().build(),
                // setembro (período de comparação)
                entry().income("Salário", 650000, Category.SALARY).on(date("2026-09-05")).build(),
                entry().expense("Aluguel", 180000, Category.HOUSING).on(date("2026-09-10")).recurrence(Recurrence.MONTHLY).build(),
                entry().expense("Supermercado", 52310, Category.GROCERIES).on(date("2026-09-07")).build(),
                entry().expense("Academia", 9990, Category.LEISURE).on(date("2026-09-03")).build());
    }

    @Test
    void should_countOnlyConfirmedEntries_in_realizedTotals() {
        var summary = DashboardCalculator.calculate(THIS_MONTH, sampleEntries(), 1);

        assertThat(summary.income().totalInCents()).isEqualTo(650000);
        assertThat(summary.income().count()).isEqualTo(1);
        assertThat(summary.expense().totalInCents()).isEqualTo(254129);
        assertThat(summary.expense().total()).isEqualTo(new BigDecimal("2541.29"));
        assertThat(summary.expense().count()).isEqualTo(5);
        assertThat(summary.balance().totalInCents()).isEqualTo(395871);
        assertThat(summary.entryCount()).isEqualTo(6);
        assertThat(summary.period().start()).isEqualTo(date("2026-10-01"));
        assertThat(summary.previousPeriod().end()).isEqualTo(date("2026-09-30"));
        assertThat(summary.preset()).isEqualTo("THIS_MONTH");
    }

    @Test
    void should_compareWithPreviousPeriod() {
        var comparison = DashboardCalculator.calculate(THIS_MONTH, sampleEntries(), 0).comparison();

        assertThat(comparison.expense().previousInCents()).isEqualTo(242300);
        assertThat(comparison.expense().deltaInCents()).isEqualTo(11829);
        assertThat(comparison.expense().deltaPercent()).isEqualTo(new BigDecimal("4.9"));
        assertThat(comparison.income().deltaPercent()).isEqualTo(new BigDecimal("0.0"));
        assertThat(comparison.balance().deltaInCents()).isEqualTo(-11829);
        assertThat(comparison.balance().deltaPercent()).isEqualTo(new BigDecimal("-2.9"));
    }

    @Test
    void should_returnNullDeltaPercent_when_previousPeriodIsZero() {
        var onlyCurrent = sampleEntries().stream().filter(t -> t.getOccurredAt().getMonthValue() == 10).toList();

        var comparison = DashboardCalculator.calculate(THIS_MONTH, onlyCurrent, 0).comparison();

        assertThat(comparison.expense().previousInCents()).isZero();
        assertThat(comparison.expense().deltaPercent()).isNull();
        assertThat(comparison.income().deltaPercent()).isNull();
    }

    @Test
    void should_groupExpensesByCategory_with_share() {
        var categories = DashboardCalculator.calculate(THIS_MONTH, sampleEntries(), 0).expensesByCategory();

        assertThat(categories).extracting("category").containsExactly("HOUSING", "GROCERIES", "LEISURE", "PHARMA");
        assertThat(categories.get(0).sharePercent()).isEqualTo(new BigDecimal("70.8"));
        assertThat(categories.get(1).totalInCents()).isEqualTo(55140);
        assertThat(categories.get(1).count()).isEqualTo(2);
        assertThat(categories.get(1).label()).isEqualTo("Mercado");
    }

    @Test
    void should_listTopExpensesAndRecentConfirmedEntries() {
        var summary = DashboardCalculator.calculate(THIS_MONTH, sampleEntries(), 0);

        assertThat(summary.topExpenses()).extracting(TransactionOutput::amountInCents)
                .containsExactly(180000L, 48790L, 9990L, 8999L, 6350L);
        assertThat(summary.recentEntries()).extracting(TransactionOutput::occurredAt).containsExactly(
                date("2026-10-11"), date("2026-10-10"), date("2026-10-07"), date("2026-10-05"),
                date("2026-10-03"), date("2026-10-02"));
        assertThat(summary.recentEntries()).allSatisfy(entry -> assertThat(entry.status()).isEqualTo("CONFIRMED"));
    }

    @Test
    void should_separatePlannedEntries_in_forecast() {
        var forecast = DashboardCalculator.calculate(THIS_MONTH, sampleEntries(), 2).forecast();

        assertThat(forecast.plannedIncome().totalInCents()).isEqualTo(95000);
        assertThat(forecast.plannedExpense().totalInCents()).isEqualTo(11990);
        assertThat(forecast.projectedBalance().totalInCents()).isEqualTo(478881);
        assertThat(forecast.plannedCount()).isEqualTo(2);
        assertThat(forecast.overduePlannedCount()).isEqualTo(2);
        assertThat(forecast.upcoming()).extracting(TransactionOutput::description).containsExactly("Internet", "Freelance");
    }

    @Test
    void should_generateDeterministicInsights_from_confirmedExpenses() {
        var insights = DashboardCalculator.calculate(THIS_MONTH, sampleEntries(), 0).insights();

        assertThat(insights).extracting(InsightOutput::kind)
                .containsExactly("TOP_CATEGORY", "LARGEST_EXPENSE", "EXPENSE_COMPARISON", "POSSIBLE_RECURRENCE");
        assertThat(insights.get(0).message()).contains("Moradia").contains("R$ 1.800,00").contains("70,8%");
        assertThat(insights.get(1).message()).contains("\"Aluguel\"").contains("10/10/2026");
        assertThat(insights.get(2).message()).contains("subiram 4,9%").contains("01/09/2026 a 30/09/2026");
        assertThat(insights.get(3).message()).startsWith("Possível cobrança recorrente: \"Academia\"").contains("30 dias");
    }

    @Test
    void should_returnZerosAndNoInsights_when_periodIsEmpty() {
        var summary = DashboardCalculator.calculate(THIS_MONTH, List.of(), 0);

        assertThat(summary.balance().totalInCents()).isZero();
        assertThat(summary.entryCount()).isZero();
        assertThat(summary.expensesByCategory()).isEmpty();
        assertThat(summary.topExpenses()).isEmpty();
        assertThat(summary.insights()).isEmpty();
        assertThat(summary.forecast().plannedCount()).isZero();
    }

    @Test
    void should_ignorePlannedEntries_in_insights() {
        var entries = List.of(entry().expense("Viagem", 500000, Category.LEISURE).on(date("2026-10-30")).planned().build());

        assertThat(DashboardCalculator.calculate(THIS_MONTH, entries, 0).insights()).isEmpty();
    }

    @Test
    void should_applyRecurrenceHeuristicLimits() {
        var base = entry().expense("Academia", 10000, Category.LEISURE).on(date("2026-09-01")).build();

        assertThat(InsightGenerator.looksRecurring(base, sameAt("2026-09-26", 10000))).isTrue();
        assertThat(InsightGenerator.looksRecurring(base, sameAt("2026-10-06", 10000))).isTrue();
        assertThat(InsightGenerator.looksRecurring(base, sameAt("2026-09-25", 10000))).isFalse();
        assertThat(InsightGenerator.looksRecurring(base, sameAt("2026-10-07", 10000))).isFalse();
        assertThat(InsightGenerator.looksRecurring(base, sameAt("2026-10-01", 11000))).isTrue();
        assertThat(InsightGenerator.looksRecurring(base, sameAt("2026-10-01", 11001))).isFalse();
        assertThat(InsightGenerator.normalize("Netflix 10/2026")).isEqualTo(InsightGenerator.normalize("NETFLIX"));
        assertThat(InsightGenerator.normalize("Farmácia - Açúcar")).isEqualTo("farmacia acucar");
    }

    private static Transaction sameAt(String date, long cents) {
        return entry().expense("Academia", cents, Category.LEISURE).on(date(date)).build();
    }

    private static LocalDate date(String value) {
        return LocalDate.parse(value);
    }
}
