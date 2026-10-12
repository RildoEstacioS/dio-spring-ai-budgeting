package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.application.input.TransactionCommand;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;
import dio.budgeting.domain.PaymentMethod;
import dio.budgeting.domain.Recurrence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import static dio.budgeting.domain.TestTransactions.CLOCK;
import static dio.budgeting.domain.TestTransactions.TODAY;
import static dio.budgeting.domain.TestTransactions.ZONE;
import static dio.budgeting.domain.TestTransactions.violatedFields;
import static org.assertj.core.api.Assertions.assertThat;

class PersistTransactionUseCaseTest {
    private InMemoryTransactionRepository transactionRepository;
    private PersistTransactionUseCase persistTransactionUseCase;

    @BeforeEach
    void setUp() {
        transactionRepository = new InMemoryTransactionRepository();
        persistTransactionUseCase = new PersistTransactionUseCase(transactionRepository, CLOCK);
    }

    @Test
    void should_applyLegacyDefaults_when_legacyInputIsUsed() {
        var lateNightInSaoPaulo = Clock.fixed(Instant.parse("2026-10-13T01:30:00Z"), ZONE);
        var useCase = new PersistTransactionUseCase(transactionRepository, lateNightInSaoPaulo);

        var output = useCase.execute(new PersistTransactionInput("Farmácia", 8000, Category.PHARMA));

        assertThat(output.type()).isEqualTo("EXPENSE");
        assertThat(output.occurredAt()).isEqualTo(LocalDate.parse("2026-10-12"));
        assertThat(output.paymentMethod()).isEqualTo("OTHER");
        assertThat(output.recurrence()).isEqualTo("NONE");
        assertThat(output.status()).isEqualTo("CONFIRMED");
        assertThat(output.notes()).isNull();
        assertThat(output.amountInCents()).isEqualTo(8000);
        assertThat(PersistTransactionUseCase.LEGACY_DEFAULT_FIELDS)
                .containsExactly("type", "occurredAt", "paymentMethod", "recurrence", "status");
    }

    @Test
    void should_rejectIncomeCategory_when_legacyInputIsUsed() {
        assertThat(violatedFields(() -> persistTransactionUseCase.execute(
                new PersistTransactionInput("Salário", 650000, Category.SALARY)))).containsExactly("category");
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    void should_rejectNonPositiveAmount_when_legacyInputIsUsed() {
        assertThat(violatedFields(() -> persistTransactionUseCase.execute(
                new PersistTransactionInput("Farmácia", 0, Category.PHARMA)))).containsExactly("amount");
    }

    @Test
    void should_createIncome_when_fullContractIsUsed() {
        var output = persistTransactionUseCase.create(command(EntryType.INCOME, Category.SALARY, TODAY, null));

        assertThat(output.type()).isEqualTo("INCOME");
        assertThat(output.category()).isEqualTo("SALARY");
        assertThat(output.status()).isEqualTo("CONFIRMED");
        assertThat(transactionRepository.findAll()).hasSize(1);
    }

    @Test
    void should_deriveStatusFromDate_when_statusIsOmitted() {
        var future = persistTransactionUseCase.create(command(EntryType.EXPENSE, Category.HOUSING, TODAY.plusDays(1), null));
        var past = persistTransactionUseCase.create(command(EntryType.EXPENSE, Category.HOUSING, TODAY.minusDays(1), null));

        assertThat(future.status()).isEqualTo("PLANNED");
        assertThat(past.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void should_rejectConfirmedFutureEntry_when_statusIsExplicit() {
        assertThat(violatedFields(() -> persistTransactionUseCase.create(
                command(EntryType.EXPENSE, Category.HOUSING, TODAY.plusDays(1), EntryStatus.CONFIRMED))))
                .containsExactly("status");
    }

    private static TransactionCommand command(EntryType type, Category category, LocalDate date, EntryStatus status) {
        return new TransactionCommand(type, "Lançamento", 10000, category, date, PaymentMethod.PIX, Recurrence.NONE,
                status, null);
    }
}
