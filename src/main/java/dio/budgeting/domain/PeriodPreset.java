package dio.budgeting.domain;

import java.time.LocalDate;
import java.time.YearMonth;

public enum PeriodPreset {
    THIS_MONTH("Este mês"),
    LAST_MONTH("Mês anterior"),
    LAST_30_DAYS("Últimos 30 dias");

    private final String label;

    PeriodPreset(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public DateRange range(LocalDate today) {
        return switch (this) {
            case THIS_MONTH -> fullMonth(YearMonth.from(today));
            case LAST_MONTH -> fullMonth(YearMonth.from(today).minusMonths(1));
            case LAST_30_DAYS -> new DateRange(today.minusDays(29), today);
        };
    }

    /**
     * Presets mensais comparam com o mês completo anterior; os demais, com o período anterior de mesma duração.
     */
    public DateRange comparisonRange(LocalDate today) {
        return switch (this) {
            case THIS_MONTH -> fullMonth(YearMonth.from(today).minusMonths(1));
            case LAST_MONTH -> fullMonth(YearMonth.from(today).minusMonths(2));
            case LAST_30_DAYS -> range(today).previousOfSameLength();
        };
    }

    private static DateRange fullMonth(YearMonth month) {
        return new DateRange(month.atDay(1), month.atEndOfMonth());
    }
}
