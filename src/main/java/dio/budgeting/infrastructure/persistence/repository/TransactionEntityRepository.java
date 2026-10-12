package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;
import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionEntityRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findAllByCategory(Category category);

    List<TransactionEntity> findAllByOccurredAtBetween(LocalDate start, LocalDate end);

    long countByStatusAndOccurredAtBefore(EntryStatus status, LocalDate date);

    @Query("""
            select t from TransactionEntity t
            where (:start is null or t.occurredAt >= :start)
              and (:end is null or t.occurredAt <= :end)
              and (:type is null or t.type = :type)
              and (:category is null or t.category = :category)
              and (:status is null or t.status = :status)
            """)
    Page<TransactionEntity> search(@Param("start") LocalDate start,
                                   @Param("end") LocalDate end,
                                   @Param("type") EntryType type,
                                   @Param("category") Category category,
                                   @Param("status") EntryStatus status,
                                   Pageable pageable);

    @Query("""
            select new dio.budgeting.infrastructure.persistence.repository.TransactionTotalRow(
                       t.type, t.status, sum(t.amount), count(t))
            from TransactionEntity t
            where (:start is null or t.occurredAt >= :start)
              and (:end is null or t.occurredAt <= :end)
              and (:type is null or t.type = :type)
              and (:category is null or t.category = :category)
              and (:status is null or t.status = :status)
            group by t.type, t.status
            """)
    List<TransactionTotalRow> sumByTypeAndStatus(@Param("start") LocalDate start,
                                                 @Param("end") LocalDate end,
                                                 @Param("type") EntryType type,
                                                 @Param("category") Category category,
                                                 @Param("status") EntryStatus status);
}
