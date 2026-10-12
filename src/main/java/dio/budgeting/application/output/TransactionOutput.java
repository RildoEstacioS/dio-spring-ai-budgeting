package dio.budgeting.application.output;

import dio.budgeting.domain.Transaction;

import java.math.BigDecimal;

public record TransactionOutput(String id, String description, String category, BigDecimal value) {
    private static final int CENTS_SCALE = 2;

    public static TransactionOutput from(Transaction transaction) {
        return new TransactionOutput(
                transaction.getId().uuid().toString(),
                transaction.getDescription(),
                transaction.getCategory().name(),
                centsToReais(transaction.getAmount()));
    }

    public static BigDecimal centsToReais(long amountInCents) {
        return BigDecimal.valueOf(amountInCents, CENTS_SCALE);
    }
}
