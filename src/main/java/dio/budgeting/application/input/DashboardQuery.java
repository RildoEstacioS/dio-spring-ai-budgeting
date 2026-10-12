package dio.budgeting.application.input;

import dio.budgeting.domain.PeriodPreset;

import java.time.LocalDate;

/**
 * Use preset OU start/end. Sem nenhum dos dois, o período padrão é o mês atual.
 */
public record DashboardQuery(PeriodPreset preset, LocalDate start, LocalDate end) {
}
