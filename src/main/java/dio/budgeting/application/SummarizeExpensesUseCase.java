package dio.budgeting.application;

import dio.budgeting.application.output.ExpenseSummaryOutput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class SummarizeExpensesUseCase {
    private static final Logger log = LoggerFactory.getLogger(SummarizeExpensesUseCase.class);

    private final TransactionRepository transactionRepository;

    public SummarizeExpensesUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Tool(name = "summarize-expenses",
            description = "Resume os gastos registrados: total gasto, quantidade de transações e maior despesa. "
                    + "Use para perguntas como 'quanto gastei', 'qual foi minha maior despesa' ou pedidos de resumo. "
                    + "Os valores retornados já estão em reais.")
    public ExpenseSummaryOutput execute(
            @ToolParam(description = "Categoria para filtrar os gastos. Omita para considerar todas as categorias",
                    required = false) Category category) {
        var transactions = findTransactions(category);

        var totalInCents = transactions.stream()
                .mapToLong(Transaction::getAmount)
                .reduce(0L, Math::addExact);

        var largestExpense = transactions.stream()
                .max(Comparator.comparingLong(Transaction::getAmount))
                .map(TransactionOutput::from)
                .orElse(null);

        log.info("Resumo de gastos gerado: categoria={}, transacoes={}",
                category == null ? "TODAS" : category, transactions.size());

        return new ExpenseSummaryOutput(
                category == null ? null : category.name(),
                TransactionOutput.centsToReais(totalInCents),
                transactions.size(),
                largestExpense);
    }

    private List<Transaction> findTransactions(Category category) {
        return category == null
                ? transactionRepository.findAll()
                : transactionRepository.findAllByCategory(category);
    }
}
