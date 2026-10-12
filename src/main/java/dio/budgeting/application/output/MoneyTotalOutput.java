package dio.budgeting.application.output;

import java.math.BigDecimal;

/**
 * Soma em reais e em centavos, com a quantidade de lançamentos considerados.
 */
public record MoneyTotalOutput(BigDecimal total, long totalInCents, long count) {
    public static MoneyTotalOutput of(long cents, long count) {
        return new MoneyTotalOutput(TransactionOutput.centsToReais(cents), cents, count);
    }
}
