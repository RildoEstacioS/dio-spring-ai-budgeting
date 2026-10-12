package dio.budgeting.domain;

import java.time.LocalDate;

/**
 * Dados editáveis de um lançamento. amount é sempre em centavos e positivo; o efeito no saldo vem de type.
 */
public record TransactionDetails(EntryType type,
                                 String description,
                                 long amount,
                                 Category category,
                                 LocalDate occurredAt,
                                 PaymentMethod paymentMethod,
                                 Recurrence recurrence,
                                 EntryStatus status,
                                 String notes) {
}
