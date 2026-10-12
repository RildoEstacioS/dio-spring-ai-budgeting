package dio.budgeting.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    Optional<Transaction> findById(TransactionId id);

    void deleteById(TransactionId id);

    List<Transaction> findAll();

    List<Transaction> findAllByCategory(Category category);

    /**
     * Lançamentos com data entre start e end (inclusivo), de qualquer tipo e situação.
     */
    List<Transaction> findAllOccurredBetween(LocalDate start, LocalDate end);

    /**
     * Página ordenada por data decrescente, depois criação decrescente e id.
     */
    TransactionSlice search(TransactionFilter filter, int page, int size);

    List<TransactionTotal> sumByTypeAndStatus(TransactionFilter filter);

    /**
     * Quantidade de lançamentos previstos com data anterior a date (aguardando confirmação).
     */
    long countPlannedBefore(LocalDate date);
}
