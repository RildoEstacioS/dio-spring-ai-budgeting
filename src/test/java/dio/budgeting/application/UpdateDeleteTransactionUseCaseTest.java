package dio.budgeting.application;

import dio.budgeting.application.input.TransactionCommand;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;
import dio.budgeting.domain.PaymentMethod;
import dio.budgeting.domain.Recurrence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static dio.budgeting.domain.TestTransactions.TODAY;
import static dio.budgeting.domain.TestTransactions.clockAt;
import static dio.budgeting.domain.TestTransactions.entry;
import static dio.budgeting.domain.TestTransactions.violatedFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateDeleteTransactionUseCaseTest {
    private static final java.time.Clock TOMORROW = clockAt(TODAY.plusDays(1));

    private InMemoryTransactionRepository transactionRepository;
    private UpdateTransactionUseCase updateTransactionUseCase;
    private DeleteTransactionUseCase deleteTransactionUseCase;
    private GetTransactionUseCase getTransactionUseCase;

    @BeforeEach
    void setUp() {
        transactionRepository = new InMemoryTransactionRepository();
        updateTransactionUseCase = new UpdateTransactionUseCase(transactionRepository, TOMORROW);
        deleteTransactionUseCase = new DeleteTransactionUseCase(transactionRepository);
        getTransactionUseCase = new GetTransactionUseCase(transactionRepository);
    }

    @Test
    void should_updateFieldsAndPreserveCreatedAt_when_transactionExists() {
        var existing = transactionRepository.save(entry().expense("Mercado", 4500, Category.GROCERIES).build());
        var id = existing.getId().uuid();

        var output = updateTransactionUseCase.execute(id, command("Mercado da semana", 5200, TODAY, EntryStatus.CONFIRMED));

        assertThat(output.id()).isEqualTo(id.toString());
        assertThat(output.description()).isEqualTo("Mercado da semana");
        assertThat(output.amountInCents()).isEqualTo(5200);
        assertThat(output.createdAt()).isEqualTo(existing.getCreatedAt());
        assertThat(output.updatedAt()).isEqualTo(TOMORROW.instant().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void should_confirmPlannedEntry_when_dateHasArrived() {
        var planned = transactionRepository.save(entry().on(TODAY.plusDays(1)).planned().build());

        var output = updateTransactionUseCase.execute(planned.getId().uuid(),
                command("Lançamento de teste", 1000, TODAY.plusDays(1), EntryStatus.CONFIRMED));

        assertThat(output.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void should_requireStatus_when_updating() {
        var existing = transactionRepository.save(entry().build());

        assertThat(violatedFields(() -> updateTransactionUseCase.execute(existing.getId().uuid(),
                command("Mercado", 1000, TODAY, null)))).containsExactly("status");
    }

    @Test
    void should_throwNotFound_when_transactionDoesNotExist() {
        var missing = UUID.randomUUID();

        assertThatThrownBy(() -> updateTransactionUseCase.execute(missing, command("X", 1000, TODAY, EntryStatus.CONFIRMED)))
                .isInstanceOf(TransactionNotFoundException.class);
        assertThatThrownBy(() -> deleteTransactionUseCase.execute(missing)).isInstanceOf(TransactionNotFoundException.class);
        assertThatThrownBy(() -> getTransactionUseCase.execute(missing)).isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void should_removeTransaction_when_deleted() {
        var existing = transactionRepository.save(entry().build());
        var other = transactionRepository.save(entry().build());

        deleteTransactionUseCase.execute(existing.getId().uuid());

        assertThat(transactionRepository.findAll()).containsExactly(other);
    }

    private static TransactionCommand command(String description, long cents, LocalDate date, EntryStatus status) {
        return new TransactionCommand(EntryType.EXPENSE, description, cents, Category.GROCERIES, date,
                PaymentMethod.DEBIT_CARD, Recurrence.NONE, status, null);
    }
}
