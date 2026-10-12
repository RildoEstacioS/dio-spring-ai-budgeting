package dio.budgeting.application.output;

import java.math.BigDecimal;

/**
 * Receitas, despesas e saldo (receitas - despesas) de um conjunto de lançamentos.
 */
public record TotalsBlockOutput(BigDecimal income,
                                long incomeInCents,
                                BigDecimal expense,
                                long expenseInCents,
                                BigDecimal balance,
                                long balanceInCents,
                                long count) {
    public static TotalsBlockOutput of(long incomeCents, long expenseCents, long count) {
        var balanceCents = Math.subtractExact(incomeCents, expenseCents);
        return new TotalsBlockOutput(
                TransactionOutput.centsToReais(incomeCents), incomeCents,
                TransactionOutput.centsToReais(expenseCents), expenseCents,
                TransactionOutput.centsToReais(balanceCents), balanceCents,
                count);
    }
}
