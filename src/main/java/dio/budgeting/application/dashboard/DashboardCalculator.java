package dio.budgeting.application.dashboard;

import dio.budgeting.application.dashboard.DashboardOutputs.CategoryTotalOutput;
import dio.budgeting.application.dashboard.DashboardOutputs.ComparisonOutput;
import dio.budgeting.application.dashboard.DashboardOutputs.ComparisonsOutput;
import dio.budgeting.application.dashboard.DashboardOutputs.DashboardSummaryOutput;
import dio.budgeting.application.dashboard.DashboardOutputs.ForecastOutput;
import dio.budgeting.application.dashboard.DashboardOutputs.PeriodOutput;
import dio.budgeting.application.output.AmountOutput;
import dio.budgeting.application.output.MoneyTotalOutput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.ReportingPeriod;
import dio.budgeting.domain.Transaction;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Cálculos determinísticos do dashboard. Realizado considera somente CONFIRMED; previsto fica em bloco separado.
 */
public final class DashboardCalculator {
    static final int TOP_EXPENSES = 5;
    static final int RECENT_ENTRIES = 10;
    static final int UPCOMING_PLANNED = 5;

    static final Comparator<Transaction> LARGEST_FIRST = Comparator
            .comparingLong(Transaction::getAmount).reversed()
            .thenComparing(Transaction::getOccurredAt, Comparator.reverseOrder())
            .thenComparing(t -> t.getId().uuid().toString());

    private static final Comparator<Transaction> MOST_RECENT_FIRST = Comparator
            .comparing(Transaction::getOccurredAt, Comparator.reverseOrder())
            .thenComparing(Transaction::getCreatedAt, Comparator.reverseOrder())
            .thenComparing(t -> t.getId().uuid().toString());

    private static final Comparator<Transaction> SOONEST_FIRST = Comparator
            .comparing(Transaction::getOccurredAt)
            .thenComparing(Transaction::getCreatedAt)
            .thenComparing(t -> t.getId().uuid().toString());

    private DashboardCalculator() {
    }

    public static DashboardSummaryOutput calculate(ReportingPeriod period,
                                                   List<Transaction> entries,
                                                   long overduePlannedCount) {
        var current = entries.stream().filter(t -> period.current().contains(t.getOccurredAt())).toList();
        var previous = entries.stream().filter(t -> period.previous().contains(t.getOccurredAt())).toList();

        var now = Totals.of(current);
        var before = Totals.of(previous);

        var balance = Math.subtractExact(now.confirmedIncome, now.confirmedExpense);
        var previousBalance = Math.subtractExact(before.confirmedIncome, before.confirmedExpense);
        var projectedBalance = Math.subtractExact(Math.addExact(balance, now.plannedIncome), now.plannedExpense);

        var confirmedExpenses = current.stream().filter(Transaction::isConfirmed).filter(Transaction::isExpense).toList();

        return new DashboardSummaryOutput(
                period.preset() == null ? null : period.preset().name(),
                period.label(),
                PeriodOutput.of(period.current()),
                PeriodOutput.of(period.previous()),
                MoneyTotalOutput.of(now.confirmedIncome, now.confirmedIncomeCount),
                MoneyTotalOutput.of(now.confirmedExpense, now.confirmedExpenseCount),
                AmountOutput.of(balance),
                now.confirmedIncomeCount + now.confirmedExpenseCount,
                new ComparisonsOutput(
                        comparison(now.confirmedIncome, before.confirmedIncome),
                        comparison(now.confirmedExpense, before.confirmedExpense),
                        comparison(balance, previousBalance)),
                expensesByCategory(confirmedExpenses, now.confirmedExpense),
                confirmedExpenses.stream().sorted(LARGEST_FIRST).limit(TOP_EXPENSES).map(TransactionOutput::from).toList(),
                current.stream().filter(Transaction::isConfirmed).sorted(MOST_RECENT_FIRST)
                        .limit(RECENT_ENTRIES).map(TransactionOutput::from).toList(),
                new ForecastOutput(
                        MoneyTotalOutput.of(now.plannedIncome, now.plannedIncomeCount),
                        MoneyTotalOutput.of(now.plannedExpense, now.plannedExpenseCount),
                        AmountOutput.of(projectedBalance),
                        now.plannedIncomeCount + now.plannedExpenseCount,
                        overduePlannedCount,
                        current.stream().filter(Transaction::isPlanned).sorted(SOONEST_FIRST)
                                .limit(UPCOMING_PLANNED).map(TransactionOutput::from).toList()),
                InsightGenerator.generate(period, current, previous));
    }

    private static ComparisonOutput comparison(long current, long previous) {
        var delta = Math.subtractExact(current, previous);
        return new ComparisonOutput(
                TransactionOutput.centsToReais(previous), previous,
                TransactionOutput.centsToReais(delta), delta,
                Percentages.change(current, previous));
    }

    private static List<CategoryTotalOutput> expensesByCategory(List<Transaction> confirmedExpenses, long expenseTotal) {
        Map<Category, long[]> totals = new EnumMap<>(Category.class);
        for (var transaction : confirmedExpenses) {
            var entry = totals.computeIfAbsent(transaction.getCategory(), c -> new long[2]);
            entry[0] = Math.addExact(entry[0], transaction.getAmount());
            entry[1]++;
        }
        return totals.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<Category, long[]> e) -> e.getValue()[0]).reversed()
                        .thenComparing(e -> e.getKey().label()))
                .map(e -> new CategoryTotalOutput(
                        e.getKey().name(),
                        e.getKey().label(),
                        TransactionOutput.centsToReais(e.getValue()[0]),
                        e.getValue()[0],
                        e.getValue()[1],
                        Percentages.share(e.getValue()[0], expenseTotal)))
                .toList();
    }

    private record Totals(long confirmedIncome, long confirmedIncomeCount,
                          long confirmedExpense, long confirmedExpenseCount,
                          long plannedIncome, long plannedIncomeCount,
                          long plannedExpense, long plannedExpenseCount) {
        static Totals of(List<Transaction> transactions) {
            long confirmedIncome = 0, confirmedIncomeCount = 0, confirmedExpense = 0, confirmedExpenseCount = 0;
            long plannedIncome = 0, plannedIncomeCount = 0, plannedExpense = 0, plannedExpenseCount = 0;
            for (var t : transactions) {
                if (t.isConfirmed() && t.isIncome()) {
                    confirmedIncome = Math.addExact(confirmedIncome, t.getAmount());
                    confirmedIncomeCount++;
                } else if (t.isConfirmed()) {
                    confirmedExpense = Math.addExact(confirmedExpense, t.getAmount());
                    confirmedExpenseCount++;
                } else if (t.isIncome()) {
                    plannedIncome = Math.addExact(plannedIncome, t.getAmount());
                    plannedIncomeCount++;
                } else {
                    plannedExpense = Math.addExact(plannedExpense, t.getAmount());
                    plannedExpenseCount++;
                }
            }
            return new Totals(confirmedIncome, confirmedIncomeCount, confirmedExpense, confirmedExpenseCount,
                    plannedIncome, plannedIncomeCount, plannedExpense, plannedExpenseCount);
        }
    }
}
