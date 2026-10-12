package dio.budgeting.domain;

import java.time.LocalDate;

/**
 * Filtro do histórico. Campos nulos não restringem a busca; datas são inclusivas.
 */
public record TransactionFilter(LocalDate start,
                                LocalDate end,
                                EntryType type,
                                Category category,
                                EntryStatus status) {

    public boolean matches(Transaction transaction) {
        return (start == null || !transaction.getOccurredAt().isBefore(start))
                && (end == null || !transaction.getOccurredAt().isAfter(end))
                && (type == null || transaction.getType() == type)
                && (category == null || transaction.getCategory() == category)
                && (status == null || transaction.getStatus() == status);
    }
}
