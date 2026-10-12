package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static dio.budgeting.domain.TestTransactions.CLOCK;
import static dio.budgeting.domain.TestTransactions.TODAY;
import static dio.budgeting.domain.TestTransactions.entry;
import static org.assertj.core.api.Assertions.assertThat;

class SummarizeExpensesUseCaseTest {
    private InMemoryTransactionRepository transactionRepository;
    private SummarizeExpensesUseCase summarizeExpensesUseCase;

    @BeforeEach
    void setUp() {
        transactionRepository = new InMemoryTransactionRepository();
        summarizeExpensesUseCase = new SummarizeExpensesUseCase(transactionRepository);
    }

    @Test
    void should_returnEmptySummary_when_noTransactionsExist() {
        var summary = summarizeExpensesUseCase.execute(null);

        assertThat(summary.category()).isNull();
        assertThat(summary.total()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.transactionCount()).isZero();
        assertThat(summary.largestExpense()).isNull();
    }

    @Test
    void should_summarizeAllTransactions_when_categoryIsNotInformed() {
        save("Farmácia", 8000, Category.PHARMA);
        save("Mercado", 12050, Category.GROCERIES);
        save("Combustível", 4000, Category.AUTO);

        var summary = summarizeExpensesUseCase.execute(null);

        assertThat(summary.category()).isNull();
        assertThat(summary.total()).isEqualTo(new BigDecimal("240.50"));
        assertThat(summary.transactionCount()).isEqualTo(3);
        assertThat(summary.largestExpense().description()).isEqualTo("Mercado");
        assertThat(summary.largestExpense().amount()).isEqualTo(new BigDecimal("120.50"));
    }

    @Test
    void should_considerOnlyInformedCategory_when_categoryIsInformed() {
        save("Farmácia", 8000, Category.PHARMA);
        save("Vitaminas", 1999, Category.PHARMA);
        save("Mercado", 12050, Category.GROCERIES);

        var summary = summarizeExpensesUseCase.execute(Category.PHARMA);

        assertThat(summary.category()).isEqualTo("PHARMA");
        assertThat(summary.total()).isEqualTo(new BigDecimal("99.99"));
        assertThat(summary.transactionCount()).isEqualTo(2);
        assertThat(summary.largestExpense().description()).isEqualTo("Farmácia");
        assertThat(summary.largestExpense().amount()).isEqualTo(new BigDecimal("80.00"));
    }

    @Test
    void should_returnEmptySummary_when_categoryHasNoTransactions() {
        save("Farmácia", 8000, Category.PHARMA);

        var summary = summarizeExpensesUseCase.execute(Category.AUTO);

        assertThat(summary.category()).isEqualTo("AUTO");
        assertThat(summary.total()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.transactionCount()).isZero();
        assertThat(summary.largestExpense()).isNull();
    }

    @Test
    void should_keepCentsPrecision_when_summingManySmallValues() {
        for (int i = 0; i < 10; i++) {
            save("Bala", 10, Category.GROCERIES);
        }

        var summary = summarizeExpensesUseCase.execute(null);

        assertThat(summary.total()).isEqualTo(new BigDecimal("1.00"));
    }

    @Test
    void should_ignoreIncomesAndPlannedEntries() {
        save("Farmácia", 8000, Category.PHARMA);
        transactionRepository.save(entry().income("Salário", 650000, Category.SALARY).build());
        transactionRepository.save(entry().expense("Aluguel", 180000, Category.HOUSING).on(TODAY.plusDays(5)).planned().build());

        var summary = summarizeExpensesUseCase.execute(null);

        assertThat(summary.total()).isEqualTo(new BigDecimal("80.00"));
        assertThat(summary.transactionCount()).isEqualTo(1);
        assertThat(summary.largestExpense().description()).isEqualTo("Farmácia");
    }

    @Test
    void should_includeTransaction_when_persistedThroughUseCase() {
        var persistTransactionUseCase = new PersistTransactionUseCase(transactionRepository, CLOCK);
        var created = persistTransactionUseCase.execute(
                new PersistTransactionInput("Farmácia", 8000, Category.PHARMA));

        var summary = summarizeExpensesUseCase.execute(Category.PHARMA);

        assertThat(summary.total()).isEqualTo(new BigDecimal("80.00"));
        assertThat(summary.transactionCount()).isEqualTo(1);
        assertThat(summary.largestExpense().id()).isEqualTo(created.id());
    }

    private void save(String description, long amountInCents, Category category) {
        transactionRepository.save(entry().expense(description, amountInCents, category).build());
    }
}
