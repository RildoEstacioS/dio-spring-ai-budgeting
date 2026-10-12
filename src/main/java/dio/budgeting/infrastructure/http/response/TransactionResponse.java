package dio.budgeting.infrastructure.http.response;

import dio.budgeting.application.output.TransactionOutput;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Campos da v1 (id, category, description, amount em reais) seguidos dos campos da v2.
 */
public record TransactionResponse(String id,
                                  String category,
                                  String description,
                                  BigDecimal amount,
                                  long amountInCents,
                                  String type,
                                  LocalDate occurredAt,
                                  String paymentMethod,
                                  String recurrence,
                                  String status,
                                  String notes,
                                  Instant createdAt,
                                  Instant updatedAt) {
    public static TransactionResponse from(TransactionOutput output) {
        return new TransactionResponse(output.id(), output.category(), output.description(), output.amount(),
                output.amountInCents(), output.type(), output.occurredAt(), output.paymentMethod(),
                output.recurrence(), output.status(), output.notes(), output.createdAt(), output.updatedAt());
    }
}
