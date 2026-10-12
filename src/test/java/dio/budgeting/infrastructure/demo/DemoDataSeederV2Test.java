package dio.budgeting.infrastructure.demo;

import dio.budgeting.application.InMemoryTransactionRepository;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryType;
import dio.budgeting.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.stream.Collectors;

import static dio.budgeting.domain.TestTransactions.CLOCK;
import static dio.budgeting.domain.TestTransactions.TODAY;
import static dio.budgeting.domain.TestTransactions.entry;
import static org.assertj.core.api.Assertions.assertThat;

class DemoDataSeederV2Test {
    private static final String DEMO_DATABASE = "budgeting_demo";
    private static final int EXPECTED_ENTRIES = 42;

    private InMemoryTransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository = new InMemoryTransactionRepository();
    }

    @Test
    void should_createRichDemoData_when_databaseMatchesAndIsEmpty() {
        var created = seeder(() -> DEMO_DATABASE).seed();

        var transactions = transactionRepository.findAll();
        assertThat(created).isEqualTo(EXPECTED_ENTRIES);
        assertThat(transactions).hasSize(EXPECTED_ENTRIES);
        assertThat(transactions).extracting(Transaction::getCategory)
                .contains(Category.SALARY, Category.FREELANCE, Category.HOUSING, Category.GROCERIES, Category.PHARMA);
        assertThat(transactions).anyMatch(Transaction::isIncome).anyMatch(Transaction::isExpense).anyMatch(Transaction::isPlanned);
        assertThat(transactions.stream().map(Transaction::getCategory).distinct().count()).isGreaterThanOrEqualTo(12);
        assertThat(transactions.stream().map(t -> YearMonth.from(t.getOccurredAt())).collect(Collectors.toSet()))
                .contains(YearMonth.from(TODAY), YearMonth.from(TODAY).minusMonths(1), YearMonth.from(TODAY).minusMonths(2));
    }

    @Test
    void should_markRecurrenceOnlyAsMetadata_for_salaryRentInternetAndStreaming() {
        seeder(() -> DEMO_DATABASE).seed();

        var recurring = transactionRepository.findAll().stream()
                .filter(t -> t.getRecurrence().isRecurring())
                .map(Transaction::getDescription)
                .collect(Collectors.toSet());

        assertThat(recurring).containsExactlyInAnyOrder("Salário", "Aluguel", "Internet fibra", "Streaming de vídeo");
    }

    @Test
    void should_planFutureEntries_and_keepAnOverduePlannedEntry() {
        seeder(() -> DEMO_DATABASE).seed();

        var transactions = transactionRepository.findAll();
        assertThat(transactions).filteredOn(t -> t.getOccurredAt().isAfter(TODAY)).allMatch(Transaction::isPlanned);
        assertThat(transactions).filteredOn(t -> t.isPlanned() && t.getOccurredAt().isBefore(TODAY)).hasSize(1);
        assertThat(transactions).filteredOn(t -> t.getType() == EntryType.INCOME && t.getCategory() == Category.SALARY).hasSize(3);
    }

    @Test
    void should_notSeed_when_databaseIsNotEmpty() {
        var existing = transactionRepository.save(entry().build());

        var created = seeder(() -> DEMO_DATABASE).seed();

        assertThat(created).isZero();
        assertThat(transactionRepository.findAll()).containsExactly(existing);
    }

    @Test
    void should_blockSeed_when_databaseDoesNotMatch() {
        assertThat(seeder(() -> "transaction").seed()).isZero();
        assertThat(seeder(() -> "transaction_demo").seed()).isZero();
        assertThat(seeder(() -> null).seed()).isZero();
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    void should_notDuplicate_when_executedTwice() {
        var seeder = seeder(() -> DEMO_DATABASE);
        seeder.seed();

        var createdOnSecondRun = seeder.seed();

        assertThat(createdOnSecondRun).isZero();
        assertThat(transactionRepository.findAll()).hasSize(EXPECTED_ENTRIES);
    }

    private DemoDataSeederV2 seeder(CurrentDatabase currentDatabase) {
        return new DemoDataSeederV2(transactionRepository, new PersistTransactionUseCase(transactionRepository, CLOCK),
                CLOCK, DEMO_DATABASE, currentDatabase);
    }
}
