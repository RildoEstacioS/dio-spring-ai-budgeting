package dio.budgeting.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Dados de teste com relógio fixo em 12/10/2026, 12h, America/Sao_Paulo.
 */
public final class TestTransactions {
    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    public static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
    public static final Clock CLOCK = clockAt(TODAY);

    private TestTransactions() {
    }

    public static Clock clockAt(LocalDate date) {
        return Clock.fixed(date.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
    }

    public static Builder entry() {
        return new Builder();
    }

    /**
     * Executa a ação e devolve os campos violados; falha se nenhuma DomainValidationException for lançada.
     */
    public static List<String> violatedFields(Runnable action) {
        try {
            action.run();
        } catch (DomainValidationException e) {
            return e.violations().stream().map(FieldViolation::field).toList();
        }
        throw new AssertionError("Esperava DomainValidationException");
    }

    public static final class Builder {
        private EntryType type = EntryType.EXPENSE;
        private String description = "Lançamento de teste";
        private long amount = 1000;
        private Category category = Category.GROCERIES;
        private LocalDate occurredAt = TODAY;
        private PaymentMethod paymentMethod = PaymentMethod.PIX;
        private Recurrence recurrence = Recurrence.NONE;
        private EntryStatus status = EntryStatus.CONFIRMED;
        private String notes;

        public Builder expense(String description, long amount, Category category) {
            this.type = EntryType.EXPENSE;
            this.description = description;
            this.amount = amount;
            this.category = category;
            return this;
        }

        public Builder income(String description, long amount, Category category) {
            this.type = EntryType.INCOME;
            this.description = description;
            this.amount = amount;
            this.category = category;
            return this;
        }

        public Builder type(EntryType type) {
            this.type = type;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder amount(long amount) {
            this.amount = amount;
            return this;
        }

        public Builder category(Category category) {
            this.category = category;
            return this;
        }

        public Builder on(LocalDate occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder recurrence(Recurrence recurrence) {
            this.recurrence = recurrence;
            return this;
        }

        public Builder status(EntryStatus status) {
            this.status = status;
            return this;
        }

        public Builder planned() {
            this.status = EntryStatus.PLANNED;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public TransactionDetails details() {
            return new TransactionDetails(type, description, amount, category, occurredAt, paymentMethod, recurrence,
                    status, notes);
        }

        public Transaction build() {
            return Transaction.create(details(), CLOCK);
        }
    }
}
