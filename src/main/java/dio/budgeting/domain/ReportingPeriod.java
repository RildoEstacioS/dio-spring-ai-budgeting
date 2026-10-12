package dio.budgeting.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Período analisado e período usado na comparação.
 */
public record ReportingPeriod(PeriodPreset preset, String label, DateRange current, DateRange previous) {
    public static final int MAX_DAYS = 366;

    public static ReportingPeriod of(PeriodPreset preset, LocalDate today) {
        return new ReportingPeriod(preset, preset.label(), preset.range(today), preset.comparisonRange(today));
    }

    public static ReportingPeriod custom(LocalDate start, LocalDate end) {
        var range = new DateRange(start, end);
        if (range.days() > MAX_DAYS) {
            throw new DomainValidationException(List.of(
                    new FieldViolation("end", "O intervalo pode ter no máximo 366 dias.")));
        }
        return new ReportingPeriod(null, "Intervalo personalizado", range, range.previousOfSameLength());
    }
}
