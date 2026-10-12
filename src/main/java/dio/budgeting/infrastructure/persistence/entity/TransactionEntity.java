package dio.budgeting.infrastructure.persistence.entity;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;
import dio.budgeting.domain.PaymentMethod;
import dio.budgeting.domain.Recurrence;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionDetails;
import dio.budgeting.domain.TransactionId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Schema mantido pelas migrações Flyway (V1 a V4). Enums gravados como VARCHAR, nunca como ENUM nativo do MySQL.
 */
@Entity
@Table(name = "transaction_entity")
@Getter
@Setter
@NoArgsConstructor
public class TransactionEntity {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 10)
    private EntryType type;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "category", nullable = false, length = 40)
    private Category category;

    @Column(name = "occurred_at", nullable = false)
    private LocalDate occurredAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "recurrence", nullable = false, length = 10)
    private Recurrence recurrence;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private EntryStatus status;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant updatedAt;

    public static TransactionEntity from(Transaction transaction) {
        var entity = new TransactionEntity();
        entity.id = transaction.getId().uuid();
        entity.type = transaction.getType();
        entity.description = transaction.getDescription();
        entity.amount = transaction.getAmount();
        entity.category = transaction.getCategory();
        entity.occurredAt = transaction.getOccurredAt();
        entity.paymentMethod = transaction.getPaymentMethod();
        entity.recurrence = transaction.getRecurrence();
        entity.status = transaction.getStatus();
        entity.notes = transaction.getNotes();
        entity.createdAt = transaction.getCreatedAt();
        entity.updatedAt = transaction.getUpdatedAt();
        return entity;
    }

    public Transaction toDomain() {
        return Transaction.restore(
                new TransactionId(id),
                new TransactionDetails(type, description, amount, category, occurredAt, paymentMethod, recurrence,
                        status, notes),
                createdAt,
                updatedAt);
    }
}
