package dio.budgeting.application.output;

import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;
import dio.budgeting.domain.TransactionTotal;

import java.util.List;

/**
 * Totais de um filtro, separando o realizado (CONFIRMED) do previsto (PLANNED).
 */
public record TransactionTotalsOutput(TotalsBlockOutput confirmed, TotalsBlockOutput planned) {
    public static TransactionTotalsOutput from(List<TransactionTotal> totals) {
        return new TransactionTotalsOutput(block(totals, EntryStatus.CONFIRMED), block(totals, EntryStatus.PLANNED));
    }

    private static TotalsBlockOutput block(List<TransactionTotal> totals, EntryStatus status) {
        long income = 0;
        long expense = 0;
        long count = 0;
        for (var total : totals) {
            if (total.status() != status) {
                continue;
            }
            if (total.type() == EntryType.INCOME) {
                income = Math.addExact(income, total.amount());
            } else {
                expense = Math.addExact(expense, total.amount());
            }
            count += total.count();
        }
        return TotalsBlockOutput.of(income, expense, count);
    }
}
