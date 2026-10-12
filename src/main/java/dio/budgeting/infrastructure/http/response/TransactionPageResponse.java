package dio.budgeting.infrastructure.http.response;

import dio.budgeting.application.output.TransactionPageOutput;
import dio.budgeting.application.output.TransactionTotalsOutput;

import java.util.List;

public record TransactionPageResponse(List<TransactionResponse> items,
                                      int page,
                                      int size,
                                      long totalItems,
                                      int totalPages,
                                      TransactionTotalsOutput totals) {
    public static TransactionPageResponse from(TransactionPageOutput output) {
        return new TransactionPageResponse(
                output.items().stream().map(TransactionResponse::from).toList(),
                output.page(), output.size(), output.totalItems(), output.totalPages(), output.totals());
    }
}
