package dio.budgeting.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static dio.budgeting.domain.TestTransactions.TODAY;
import static dio.budgeting.domain.TestTransactions.violatedFields;
import static org.assertj.core.api.Assertions.assertThat;

class ReportingPeriodTest {

    @Test
    void should_compareFullMonths_when_presetIsThisMonth() {
        var period = ReportingPeriod.of(PeriodPreset.THIS_MONTH, TODAY);

        assertThat(period.current()).isEqualTo(range("2026-10-01", "2026-10-31"));
        assertThat(period.previous()).isEqualTo(range("2026-09-01", "2026-09-30"));
        assertThat(period.label()).isEqualTo("Este mês");
    }

    @Test
    void should_compareFullMonths_when_presetIsLastMonth() {
        var period = ReportingPeriod.of(PeriodPreset.LAST_MONTH, TODAY);

        assertThat(period.current()).isEqualTo(range("2026-09-01", "2026-09-30"));
        assertThat(period.previous()).isEqualTo(range("2026-08-01", "2026-08-31"));
    }

    @Test
    void should_compareSameLength_when_presetIsLast30Days() {
        var period = ReportingPeriod.of(PeriodPreset.LAST_30_DAYS, TODAY);

        assertThat(period.current()).isEqualTo(range("2026-09-13", "2026-10-12"));
        assertThat(period.previous()).isEqualTo(range("2026-08-14", "2026-09-12"));
        assertThat(period.current().days()).isEqualTo(30);
        assertThat(period.previous().days()).isEqualTo(30);
    }

    @Test
    void should_crossYearBoundary_when_monthIsJanuary() {
        var period = ReportingPeriod.of(PeriodPreset.THIS_MONTH, LocalDate.parse("2027-01-15"));

        assertThat(period.previous()).isEqualTo(range("2026-12-01", "2026-12-31"));
    }

    @Test
    void should_handleLeapYear_when_lastMonthIsFebruary() {
        var period = ReportingPeriod.of(PeriodPreset.LAST_MONTH, LocalDate.parse("2028-03-10"));

        assertThat(period.current()).isEqualTo(range("2028-02-01", "2028-02-29"));
        assertThat(period.previous()).isEqualTo(range("2028-01-01", "2028-01-31"));
    }

    @Test
    void should_comparePreviousSameLength_when_rangeIsCustom() {
        var period = ReportingPeriod.custom(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-10"));

        assertThat(period.preset()).isNull();
        assertThat(period.previous()).isEqualTo(range("2026-09-21", "2026-09-30"));
    }

    @Test
    void should_limitCustomRange_to366Days() {
        assertThat(ReportingPeriod.custom(LocalDate.parse("2026-01-01"), LocalDate.parse("2027-01-01")).current().days())
                .isEqualTo(366);
        assertThat(violatedFields(() -> ReportingPeriod.custom(LocalDate.parse("2026-01-01"), LocalDate.parse("2027-01-02"))))
                .containsExactly("end");
    }

    @Test
    void should_rejectRange_when_startIsAfterEnd() {
        assertThat(violatedFields(() -> new DateRange(LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-01"))))
                .containsExactly("end");
    }

    private static DateRange range(String start, String end) {
        return new DateRange(LocalDate.parse(start), LocalDate.parse(end));
    }
}
