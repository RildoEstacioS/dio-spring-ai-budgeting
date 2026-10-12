package dio.budgeting.application.input;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;

import java.time.LocalDate;

public record TransactionSearchQuery(LocalDate start,
                                     LocalDate end,
                                     EntryType type,
                                     Category category,
                                     EntryStatus status,
                                     int page,
                                     int size) {
}
