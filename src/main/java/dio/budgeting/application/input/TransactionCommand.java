package dio.budgeting.application.input;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;
import dio.budgeting.domain.PaymentMethod;
import dio.budgeting.domain.Recurrence;

import java.time.LocalDate;

/**
 * Contrato completo de criação/edição. amount em centavos. status nulo na criação é derivado da data.
 */
public record TransactionCommand(EntryType type,
                                 String description,
                                 long amount,
                                 Category category,
                                 LocalDate occurredAt,
                                 PaymentMethod paymentMethod,
                                 Recurrence recurrence,
                                 EntryStatus status,
                                 String notes) {
}
