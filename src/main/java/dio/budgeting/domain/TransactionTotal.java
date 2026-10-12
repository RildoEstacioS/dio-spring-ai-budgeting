package dio.budgeting.domain;

/**
 * Soma em centavos e quantidade de lançamentos de um tipo e situação.
 */
public record TransactionTotal(EntryType type, EntryStatus status, long amount, long count) {
}
