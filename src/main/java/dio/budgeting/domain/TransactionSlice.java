package dio.budgeting.domain;

import java.util.List;

/**
 * Uma página do histórico e o total de itens que atendem ao filtro.
 */
public record TransactionSlice(List<Transaction> items, long totalItems) {
}
