package dio.budgeting.application.output;

import java.math.BigDecimal;

/**
 * Valor em reais e em centavos.
 */
public record AmountOutput(BigDecimal total, long totalInCents) {
    public static AmountOutput of(long cents) {
        return new AmountOutput(TransactionOutput.centsToReais(cents), cents);
    }
}
