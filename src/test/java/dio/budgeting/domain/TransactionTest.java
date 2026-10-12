package dio.budgeting.domain;

import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;

import static dio.budgeting.domain.TestTransactions.CLOCK;
import static dio.budgeting.domain.TestTransactions.TODAY;
import static dio.budgeting.domain.TestTransactions.clockAt;
import static dio.budgeting.domain.TestTransactions.entry;
import static dio.budgeting.domain.TestTransactions.violatedFields;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {

    @Test
    void should_createTransaction_when_detailsAreValid() {
        var transaction = entry().expense("  Mercado  ", 4500, Category.GROCERIES).build();

        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getDescription()).isEqualTo("Mercado");
        assertThat(transaction.getAmount()).isEqualTo(4500);
        assertThat(transaction.isExpense()).isTrue();
        assertThat(transaction.isConfirmed()).isTrue();
        assertThat(transaction.getCreatedAt()).isEqualTo(CLOCK.instant().truncatedTo(ChronoUnit.MICROS));
        assertThat(transaction.getUpdatedAt()).isEqualTo(transaction.getCreatedAt());
    }

    @Test
    void should_rejectDescription_when_blankOrTooLong() {
        assertThat(violatedFields(() -> entry().description("   ").build())).containsExactly("description");
        assertThat(violatedFields(() -> entry().description("a".repeat(256)).build())).containsExactly("description");
    }

    @Test
    void should_rejectAmount_when_zeroOrNegative() {
        assertThat(violatedFields(() -> entry().amount(0).build())).containsExactly("amount");
        assertThat(violatedFields(() -> entry().amount(-100).build())).containsExactly("amount");
    }

    @Test
    void should_rejectCategory_when_incompatibleWithType() {
        try {
            entry().expense("Salário", 650000, Category.SALARY).build();
        } catch (DomainValidationException e) {
            assertThat(e.violations()).singleElement().satisfies(v -> {
                assertThat(v.field()).isEqualTo("category");
                assertThat(v.message()).contains("Salário").contains("despesa");
            });
            return;
        }
        throw new AssertionError("Esperava DomainValidationException");
    }

    @Test
    void should_rejectDate_when_outOfAllowedRange() {
        assertThat(violatedFields(() -> entry().on(Transaction.MIN_OCCURRED_AT.minusDays(1)).build()))
                .containsExactly("occurredAt");
        assertThat(violatedFields(() -> entry().on(TODAY.plusMonths(12).plusDays(1)).planned().build()))
                .containsExactly("occurredAt");
    }

    @Test
    void should_acceptBoundaryDates_when_insideAllowedRange() {
        assertThat(entry().on(Transaction.MIN_OCCURRED_AT).build().getOccurredAt()).isEqualTo(Transaction.MIN_OCCURRED_AT);
        assertThat(entry().on(TODAY.plusMonths(12)).planned().build().isPlanned()).isTrue();
    }

    @Test
    void should_requirePlanned_when_dateIsInTheFuture() {
        assertThat(violatedFields(() -> entry().on(TODAY.plusDays(1)).build())).containsExactly("status");
        assertThat(entry().on(TODAY.plusDays(1)).planned().build().isPlanned()).isTrue();
    }

    @Test
    void should_acceptPlanned_when_dateIsTodayOrPast() {
        assertThat(entry().on(TODAY).planned().build().isPlanned()).isTrue();
        assertThat(entry().on(TODAY.minusDays(10)).planned().build().isPlanned()).isTrue();
    }

    @Test
    void should_normalizeNotes_when_blankAndRejectWhenTooLong() {
        assertThat(entry().notes("   ").build().getNotes()).isNull();
        assertThat(violatedFields(() -> entry().notes("a".repeat(501)).build())).containsExactly("notes");
    }

    @Test
    void should_reportAllViolations_when_severalFieldsAreInvalid() {
        assertThat(violatedFields(() -> entry().description("").amount(0).build()))
                .containsExactly("description", "amount");
    }

    @Test
    void should_preserveIdAndCreatedAt_when_updated() {
        var transaction = entry().on(TODAY.minusDays(2)).planned().build();
        var id = transaction.getId();
        var createdAt = transaction.getCreatedAt();
        var tomorrow = clockAt(TODAY.plusDays(1));

        transaction.update(entry().expense("Mercado do mês", 9000, Category.GROCERIES).on(TODAY.minusDays(2)).details(), tomorrow);

        assertThat(transaction.getId()).isEqualTo(id);
        assertThat(transaction.getCreatedAt()).isEqualTo(createdAt);
        assertThat(transaction.getUpdatedAt()).isEqualTo(tomorrow.instant().truncatedTo(ChronoUnit.MICROS));
        assertThat(transaction.getDescription()).isEqualTo("Mercado do mês");
        assertThat(transaction.isConfirmed()).isTrue();
    }

    @Test
    void should_rejectUpdate_when_futureEntryIsConfirmed() {
        var transaction = entry().on(TODAY.plusDays(5)).planned().build();

        assertThat(violatedFields(() -> transaction.update(entry().on(TODAY.plusDays(5)).details(), CLOCK)))
                .containsExactly("status");
        assertThat(transaction.isPlanned()).isTrue();
    }
}
