package dio.budgeting.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Lançamento financeiro (receita ou despesa). Valores em centavos; regras de negócio validadas na criação e na edição.
 */
public class Transaction {
    public static final LocalDate MIN_OCCURRED_AT = LocalDate.of(2000, 1, 1);
    public static final int MAX_MONTHS_AHEAD = 12;
    public static final int MAX_DESCRIPTION_LENGTH = 255;
    public static final int MAX_NOTES_LENGTH = 500;

    private final TransactionId id;
    private final Instant createdAt;
    private TransactionDetails details;
    private Instant updatedAt;

    private Transaction(TransactionId id, TransactionDetails details, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.details = details;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Transaction create(TransactionDetails details, Clock clock) {
        var validated = validate(details, LocalDate.now(clock));
        var now = now(clock);
        return new Transaction(new TransactionId(), validated, now, now);
    }

    /**
     * Reconstrói um lançamento já persistido, sem reaplicar regras dependentes da data atual.
     */
    public static Transaction restore(TransactionId id, TransactionDetails details, Instant createdAt, Instant updatedAt) {
        return new Transaction(id, details, createdAt, updatedAt);
    }

    /**
     * Edição explícita: preserva id e createdAt e atualiza updatedAt.
     */
    public void update(TransactionDetails newDetails, Clock clock) {
        this.details = validate(newDetails, LocalDate.now(clock));
        this.updatedAt = now(clock);
    }

    public TransactionId getId() {
        return id;
    }

    public TransactionDetails details() {
        return details;
    }

    public EntryType getType() {
        return details.type();
    }

    public String getDescription() {
        return details.description();
    }

    public long getAmount() {
        return details.amount();
    }

    public Category getCategory() {
        return details.category();
    }

    public LocalDate getOccurredAt() {
        return details.occurredAt();
    }

    public PaymentMethod getPaymentMethod() {
        return details.paymentMethod();
    }

    public Recurrence getRecurrence() {
        return details.recurrence();
    }

    public EntryStatus getStatus() {
        return details.status();
    }

    public String getNotes() {
        return details.notes();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isIncome() {
        return details.type() == EntryType.INCOME;
    }

    public boolean isExpense() {
        return details.type() == EntryType.EXPENSE;
    }

    public boolean isConfirmed() {
        return details.status() == EntryStatus.CONFIRMED;
    }

    public boolean isPlanned() {
        return details.status() == EntryStatus.PLANNED;
    }

    private static Instant now(Clock clock) {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    static TransactionDetails validate(TransactionDetails details, LocalDate today) {
        List<FieldViolation> violations = new ArrayList<>();

        var description = details.description() == null ? "" : details.description().strip();
        if (description.isEmpty()) {
            violations.add(new FieldViolation("description", "Informe uma descrição."));
        } else if (description.length() > MAX_DESCRIPTION_LENGTH) {
            violations.add(new FieldViolation("description", "A descrição deve ter no máximo 255 caracteres."));
        }

        if (details.amount() <= 0) {
            violations.add(new FieldViolation("amount", "O valor deve ser maior que zero."));
        }

        if (details.type() == null) {
            violations.add(new FieldViolation("type", "Informe se é receita ou despesa."));
        }

        if (details.category() == null) {
            violations.add(new FieldViolation("category", "Informe a categoria."));
        } else if (details.type() != null && !details.category().isCompatibleWith(details.type())) {
            violations.add(new FieldViolation("category", "A categoria %s não é compatível com %s."
                    .formatted(details.category().label(), details.type().label().toLowerCase())));
        }

        var occurredAt = details.occurredAt();
        if (occurredAt == null) {
            violations.add(new FieldViolation("occurredAt", "Informe a data do lançamento."));
        } else if (occurredAt.isBefore(MIN_OCCURRED_AT)) {
            violations.add(new FieldViolation("occurredAt", "A data deve ser a partir de 01/01/2000."));
        } else if (occurredAt.isAfter(today.plusMonths(MAX_MONTHS_AHEAD))) {
            violations.add(new FieldViolation("occurredAt", "A data pode estar no máximo 12 meses à frente."));
        }

        if (details.paymentMethod() == null) {
            violations.add(new FieldViolation("paymentMethod", "Informe o meio de pagamento."));
        }

        if (details.recurrence() == null) {
            violations.add(new FieldViolation("recurrence", "Informe a recorrência."));
        }

        if (details.status() == null) {
            violations.add(new FieldViolation("status", "Informe se o lançamento é realizado ou previsto."));
        } else if (details.status() == EntryStatus.CONFIRMED && occurredAt != null && occurredAt.isAfter(today)) {
            violations.add(new FieldViolation("status", "Lançamentos com data futura devem ser registrados como previstos."));
        }

        var notes = details.notes() == null ? null : details.notes().strip();
        if (notes != null && notes.isEmpty()) {
            notes = null;
        }
        if (notes != null && notes.length() > MAX_NOTES_LENGTH) {
            violations.add(new FieldViolation("notes", "A observação deve ter no máximo 500 caracteres."));
        }

        if (!violations.isEmpty()) {
            throw new DomainValidationException(violations);
        }

        return new TransactionDetails(details.type(), description, details.amount(), details.category(), occurredAt,
                details.paymentMethod(), details.recurrence(), details.status(), notes);
    }
}
