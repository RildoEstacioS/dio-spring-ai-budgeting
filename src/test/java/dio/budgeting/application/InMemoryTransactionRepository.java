package dio.budgeting.application;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Repositório falso em memória para testar casos de uso sem Spring, banco ou Docker.
 */
public class InMemoryTransactionRepository implements TransactionRepository {
    private final List<Transaction> transactions = new ArrayList<>();

    @Override
    public Transaction save(Transaction transaction) {
        transactions.add(transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findAll() {
        return List.copyOf(transactions);
    }

    @Override
    public List<Transaction> findAllByCategory(Category category) {
        return transactions.stream()
                .filter(transaction -> transaction.getCategory() == category)
                .toList();
    }
}
