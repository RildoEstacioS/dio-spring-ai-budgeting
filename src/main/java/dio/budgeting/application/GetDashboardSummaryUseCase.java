package dio.budgeting.application;

import dio.budgeting.application.dashboard.DashboardCalculator;
import dio.budgeting.application.dashboard.DashboardOutputs.DashboardSummaryOutput;
import dio.budgeting.application.input.DashboardQuery;
import dio.budgeting.domain.DomainValidationException;
import dio.budgeting.domain.FieldViolation;
import dio.budgeting.domain.PeriodPreset;
import dio.budgeting.domain.ReportingPeriod;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class GetDashboardSummaryUseCase {
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public GetDashboardSummaryUseCase(TransactionRepository transactionRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    public DashboardSummaryOutput execute(DashboardQuery query) {
        var today = LocalDate.now(clock);
        var period = resolve(query, today);
        var entries = transactionRepository.findAllOccurredBetween(period.previous().start(), period.current().end());
        return DashboardCalculator.calculate(period, entries, transactionRepository.countPlannedBefore(today));
    }

    private static ReportingPeriod resolve(DashboardQuery query, LocalDate today) {
        var hasDates = query.start() != null || query.end() != null;
        if (query.preset() != null && hasDates) {
            throw new DomainValidationException(List.of(
                    new FieldViolation("period", "Use period ou start/end, não os dois.")));
        }
        if (query.preset() != null) {
            return ReportingPeriod.of(query.preset(), today);
        }
        if (hasDates) {
            if (query.start() == null || query.end() == null) {
                throw new DomainValidationException(List.of(
                        new FieldViolation(query.start() == null ? "start" : "end", "Informe as datas inicial e final.")));
            }
            return ReportingPeriod.custom(query.start(), query.end());
        }
        return ReportingPeriod.of(PeriodPreset.THIS_MONTH, today);
    }
}
