package dio.budgeting.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Intervalo de datas inclusivo nas duas pontas.
 */
public record DateRange(LocalDate start, LocalDate end) {
    public DateRange {
        if (start == null || end == null) {
            throw new DomainValidationException(List.of(new FieldViolation("start", "Informe as datas inicial e final.")));
        }
        if (start.isAfter(end)) {
            throw new DomainValidationException(List.of(new FieldViolation("end", "A data final deve ser igual ou posterior à inicial.")));
        }
    }

    public long days() {
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * Período imediatamente anterior, com a mesma quantidade de dias.
     */
    public DateRange previousOfSameLength() {
        return new DateRange(start.minusDays(days()), start.minusDays(1));
    }
}
