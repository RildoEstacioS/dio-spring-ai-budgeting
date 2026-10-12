package dio.budgeting.application;

import dio.budgeting.application.input.TransactionSearchQuery;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static dio.budgeting.domain.TestTransactions.entry;
import static dio.budgeting.domain.TestTransactions.violatedFields;
import static org.assertj.core.api.Assertions.assertThat;

class SearchTransactionsUseCaseTest {
    private InMemoryTransactionRepository transactionRepository;
    private SearchTransactionsUseCase searchTransactionsUseCase;

    @BeforeEach
    void setUp() {
        transactionRepository = new InMemoryTransactionRepository();
        searchTransactionsUseCase = new SearchTransactionsUseCase(transactionRepository);
        transactionRepository.save(entry().income("Salário", 650000, Category.SALARY).on(date("2026-10-05")).build());
        transactionRepository.save(entry().expense("Mercado", 10000, Category.GROCERIES).on(date("2026-10-07")).build());
        transactionRepository.save(entry().expense("Farmácia", 5000, Category.PHARMA).on(date("2026-09-20")).build());
        transactionRepository.save(entry().expense("Aluguel", 180000, Category.HOUSING).on(date("2026-11-10")).planned().build());
    }

    @Test
    void should_paginateAndSeparateTotals_when_noFilterIsGiven() {
        var page = searchTransactionsUseCase.execute(query(null, null, null, null, null, 0, 2));

        assertThat(page.totalItems()).isEqualTo(4);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.items()).extracting("description").containsExactly("Aluguel", "Mercado");

        assertThat(page.totals().confirmed().incomeInCents()).isEqualTo(650000);
        assertThat(page.totals().confirmed().expenseInCents()).isEqualTo(15000);
        assertThat(page.totals().confirmed().balanceInCents()).isEqualTo(635000);
        assertThat(page.totals().confirmed().balance()).isEqualTo(new BigDecimal("6350.00"));
        assertThat(page.totals().confirmed().count()).isEqualTo(3);
        assertThat(page.totals().planned().expenseInCents()).isEqualTo(180000);
        assertThat(page.totals().planned().balanceInCents()).isEqualTo(-180000);
        assertThat(page.totals().planned().count()).isEqualTo(1);
    }

    @Test
    void should_returnNextPage_when_pageIsOne() {
        var page = searchTransactionsUseCase.execute(query(null, null, null, null, null, 1, 2));

        assertThat(page.items()).extracting("description").containsExactly("Salário", "Farmácia");
    }

    @Test
    void should_applyAllFilters_when_given() {
        var october = searchTransactionsUseCase.execute(
                query(date("2026-10-01"), date("2026-10-31"), EntryType.EXPENSE, null, null, 0, 20));
        var planned = searchTransactionsUseCase.execute(query(null, null, null, null, EntryStatus.PLANNED, 0, 20));
        var pharma = searchTransactionsUseCase.execute(query(null, null, null, Category.PHARMA, null, 0, 20));

        assertThat(october.items()).extracting("description").containsExactly("Mercado");
        assertThat(october.totals().confirmed().expenseInCents()).isEqualTo(10000);
        assertThat(planned.items()).extracting("description").containsExactly("Aluguel");
        assertThat(pharma.items()).extracting("description").containsExactly("Farmácia");
    }

    @Test
    void should_returnEmptyPage_when_nothingMatches() {
        var page = searchTransactionsUseCase.execute(query(null, null, null, Category.EDUCATION, null, 0, 20));

        assertThat(page.items()).isEmpty();
        assertThat(page.totalItems()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThat(page.totals().confirmed().balanceInCents()).isZero();
    }

    @Test
    void should_rejectInvalidPaging_and_invertedDates() {
        assertThat(violatedFields(() -> searchTransactionsUseCase.execute(query(null, null, null, null, null, 0, 101))))
                .containsExactly("size");
        assertThat(violatedFields(() -> searchTransactionsUseCase.execute(query(null, null, null, null, null, -1, 20))))
                .containsExactly("page");
        assertThat(violatedFields(() -> searchTransactionsUseCase.execute(
                query(date("2026-10-31"), date("2026-10-01"), null, null, null, 0, 20)))).containsExactly("end");
    }

    private static TransactionSearchQuery query(LocalDate start, LocalDate end, EntryType type, Category category,
                                                EntryStatus status, int page, int size) {
        return new TransactionSearchQuery(start, end, type, category, status, page, size);
    }

    private static LocalDate date(String value) {
        return LocalDate.parse(value);
    }
}
