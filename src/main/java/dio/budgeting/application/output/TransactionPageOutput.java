package dio.budgeting.application.output;

import java.util.List;

public record TransactionPageOutput(List<TransactionOutput> items,
                                    int page,
                                    int size,
                                    long totalItems,
                                    int totalPages,
                                    TransactionTotalsOutput totals) {
}
