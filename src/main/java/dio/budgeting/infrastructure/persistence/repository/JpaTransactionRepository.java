package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionFilter;
import dio.budgeting.domain.TransactionId;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.domain.TransactionSlice;
import dio.budgeting.domain.TransactionTotal;
import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaTransactionRepository implements TransactionRepository {
    private static final Sort HISTORY_ORDER = Sort.by(
            Sort.Order.desc("occurredAt"),
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final TransactionEntityRepository transactionEntityRepository;

    public JpaTransactionRepository(TransactionEntityRepository transactionEntityRepository) {
        this.transactionEntityRepository = transactionEntityRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        var entity = TransactionEntity.from(transaction);
        return transactionEntityRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Transaction> findById(TransactionId id) {
        return transactionEntityRepository.findById(id.uuid()).map(TransactionEntity::toDomain);
    }

    @Override
    public void deleteById(TransactionId id) {
        transactionEntityRepository.deleteById(id.uuid());
    }

    @Override
    public List<Transaction> findAll() {
        return transactionEntityRepository.findAll().stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }

    @Override
    public List<Transaction> findAllByCategory(Category category) {
        return transactionEntityRepository.findAllByCategory(category)
                .stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }

    @Override
    public List<Transaction> findAllOccurredBetween(LocalDate start, LocalDate end) {
        return transactionEntityRepository.findAllByOccurredAtBetween(start, end).stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }

    @Override
    public TransactionSlice search(TransactionFilter filter, int page, int size) {
        var result = transactionEntityRepository.search(filter.start(), filter.end(), filter.type(),
                filter.category(), filter.status(), PageRequest.of(page, size, HISTORY_ORDER));
        return new TransactionSlice(result.getContent().stream().map(TransactionEntity::toDomain).toList(),
                result.getTotalElements());
    }

    @Override
    public List<TransactionTotal> sumByTypeAndStatus(TransactionFilter filter) {
        return transactionEntityRepository.sumByTypeAndStatus(filter.start(), filter.end(), filter.type(),
                        filter.category(), filter.status()).stream()
                .map(row -> new TransactionTotal(row.type(), row.status(), row.amount(), row.count()))
                .toList();
    }

    @Override
    public long countPlannedBefore(LocalDate date) {
        return transactionEntityRepository.countByStatusAndOccurredAtBefore(EntryStatus.PLANNED, date);
    }
}
