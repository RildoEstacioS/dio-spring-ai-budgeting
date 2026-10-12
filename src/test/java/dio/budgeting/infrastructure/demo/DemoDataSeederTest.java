package dio.budgeting.infrastructure.demo;

import dio.budgeting.application.InMemoryTransactionRepository;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataSeederTest {
    private InMemoryTransactionRepository transactionRepository;
    private DemoDataSeeder demoDataSeeder;

    @BeforeEach
    void setUp() {
        transactionRepository = new InMemoryTransactionRepository();
        demoDataSeeder = new DemoDataSeeder(transactionRepository, new PersistTransactionUseCase(transactionRepository));
    }

    @Test
    void should_createDemoTransactions_when_repositoryIsEmpty() {
        var created = demoDataSeeder.seed();

        var transactions = transactionRepository.findAll();
        assertThat(created).isEqualTo(DemoDataSeeder.DEMO_TRANSACTIONS.size());
        assertThat(transactions).hasSize(DemoDataSeeder.DEMO_TRANSACTIONS.size());
        assertThat(transactions).allSatisfy(transaction -> assertThat(transaction.getAmount()).isPositive());
        assertThat(transactions.stream().map(Transaction::getCategory).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(Category.values());
    }

    @Test
    void should_keepExistingData_when_repositoryIsNotEmpty() {
        var existing = transactionRepository.save(new Transaction("Farmácia", 8000, Category.PHARMA));

        var created = demoDataSeeder.seed();

        assertThat(created).isZero();
        assertThat(transactionRepository.findAll()).containsExactly(existing);
    }

    @Test
    void should_notDuplicate_when_executedTwice() {
        demoDataSeeder.seed();

        var createdOnSecondRun = demoDataSeeder.seed();

        assertThat(createdOnSecondRun).isZero();
        assertThat(transactionRepository.findAll()).hasSize(DemoDataSeeder.DEMO_TRANSACTIONS.size());
    }
}
