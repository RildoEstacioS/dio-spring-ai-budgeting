package dio.budgeting.application;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionFilter;
import dio.budgeting.domain.TransactionId;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.domain.TransactionSlice;
import dio.budgeting.domain.TransactionTotal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositório falso em memória para testar casos de uso sem Spring, banco ou Docker.
 * Segue a mesma ordenação e as mesmas regras de filtro do adaptador JPA.
 */
public class InMemoryTransactionRepository implements TransactionRepository {
    private static final Comparator<Transaction> HISTORY_ORDER = Comparator
            .comparing(Transaction::getOccurredAt, Comparator.reverseOrder())
            .thenComparing(Transaction::getCreatedAt, Comparator.reverseOrder())
            .thenComparing(t -> t.getId().uuid().toString(), Comparator.reverseOrder());

    private final Map<TransactionId, Transaction> transactions = new LinkedHashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        transactions.put(transaction.getId(), transaction);
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(TransactionId id) {
        return Optional.ofNullable(transactions.get(id));
    }

    @Override
    public void deleteById(TransactionId id) {
        transactions.remove(id);
    }

    @Override
    public List<Transaction> findAll() {
        return List.copyOf(transactions.values());
    }

    @Override
    public List<Transaction> findAllByCategory(Category category) {
        return transactions.values().stream()
                .filter(transaction -> transaction.getCategory() == category)
                .toList();
    }

    @Override
    public List<Transaction> findAllOccurredBetween(LocalDate start, LocalDate end) {
        return transactions.values().stream()
                .filter(t -> !t.getOccurredAt().isBefore(start) && !t.getOccurredAt().isAfter(end))
                .toList();
    }

    @Override
    public TransactionSlice search(TransactionFilter filter, int page, int size) {
        var matching = transactions.values().stream().filter(filter::matches).sorted(HISTORY_ORDER).toList();
        var items = matching.stream().skip((long) page * size).limit(size).toList();
        return new TransactionSlice(items, matching.size());
    }

    @Override
    public List<TransactionTotal> sumByTypeAndStatus(TransactionFilter filter) {
        List<TransactionTotal> totals = new ArrayList<>();
        for (var type : EntryType.values()) {
            for (var status : EntryStatus.values()) {
                var group = transactions.values().stream()
                        .filter(filter::matches)
                        .filter(t -> t.getType() == type && t.getStatus() == status)
                        .toList();
                if (!group.isEmpty()) {
                    totals.add(new TransactionTotal(type, status,
                            group.stream().mapToLong(Transaction::getAmount).sum(), group.size()));
                }
            }
        }
        return totals;
    }

    @Override
    public long countPlannedBefore(LocalDate date) {
        return transactions.values().stream()
                .filter(Transaction::isPlanned)
                .filter(t -> t.getOccurredAt().isBefore(date))
                .count();
    }
}
