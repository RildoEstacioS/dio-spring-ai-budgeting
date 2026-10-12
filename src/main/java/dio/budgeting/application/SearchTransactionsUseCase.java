package dio.budgeting.application;

import dio.budgeting.application.input.TransactionSearchQuery;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.application.output.TransactionPageOutput;
import dio.budgeting.application.output.TransactionTotalsOutput;
import dio.budgeting.domain.DomainValidationException;
import dio.budgeting.domain.FieldViolation;
import dio.budgeting.domain.TransactionFilter;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchTransactionsUseCase {
    public static final int MAX_PAGE_SIZE = 100;

    private final TransactionRepository transactionRepository;

    public SearchTransactionsUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionPageOutput execute(TransactionSearchQuery query) {
        validate(query);

        var filter = new TransactionFilter(query.start(), query.end(), query.type(), query.category(), query.status());
        var slice = transactionRepository.search(filter, query.page(), query.size());
        var totals = TransactionTotalsOutput.from(transactionRepository.sumByTypeAndStatus(filter));
        var totalPages = (int) ((slice.totalItems() + query.size() - 1) / query.size());

        return new TransactionPageOutput(
                slice.items().stream().map(TransactionOutput::from).toList(),
                query.page(), query.size(), slice.totalItems(), totalPages, totals);
    }

    private static void validate(TransactionSearchQuery query) {
        List<FieldViolation> violations = new ArrayList<>();
        if (query.page() < 0) {
            violations.add(new FieldViolation("page", "A página deve ser 0 ou maior."));
        }
        if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            violations.add(new FieldViolation("size", "O tamanho da página deve estar entre 1 e 100."));
        }
        if (query.start() != null && query.end() != null && query.start().isAfter(query.end())) {
            violations.add(new FieldViolation("end", "A data final deve ser igual ou posterior à inicial."));
        }
        if (!violations.isEmpty()) {
            throw new DomainValidationException(violations);
        }
    }
}
