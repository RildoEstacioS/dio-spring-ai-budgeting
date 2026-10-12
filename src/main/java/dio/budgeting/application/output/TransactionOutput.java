package dio.budgeting.application.output;

import dio.budgeting.domain.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Lançamento com valor em reais (amount) e em centavos (amountInCents).
 */
public record TransactionOutput(String id,
                                String type,
                                String description,
                                String category,
                                BigDecimal amount,
                                long amountInCents,
                                LocalDate occurredAt,
                                String paymentMethod,
                                String recurrence,
                                String status,
                                String notes,
                                Instant createdAt,
                                Instant updatedAt) {
    private static final int CENTS_SCALE = 2;

    public static TransactionOutput from(Transaction transaction) {
        return new TransactionOutput(
                transaction.getId().uuid().toString(),
                transaction.getType().name(),
                transaction.getDescription(),
                transaction.getCategory().name(),
                centsToReais(transaction.getAmount()),
                transaction.getAmount(),
                transaction.getOccurredAt(),
                transaction.getPaymentMethod().name(),
                transaction.getRecurrence().name(),
                transaction.getStatus().name(),
                transaction.getNotes(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt());
    }

    public static BigDecimal centsToReais(long amountInCents) {
        return BigDecimal.valueOf(amountInCents, CENTS_SCALE);
    }
}
