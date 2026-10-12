package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;

public record TransactionTotalRow(EntryType type, EntryStatus status, Long amount, Long count) {
}
