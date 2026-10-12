package dio.budgeting.infrastructure.http;

import dio.budgeting.application.GetDashboardSummaryUseCase;
import dio.budgeting.application.dashboard.DashboardOutputs.DashboardSummaryOutput;
import dio.budgeting.application.input.DashboardQuery;
import dio.budgeting.domain.PeriodPreset;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    private final GetDashboardSummaryUseCase getDashboardSummaryUseCase;

    public DashboardController(GetDashboardSummaryUseCase getDashboardSummaryUseCase) {
        this.getDashboardSummaryUseCase = getDashboardSummaryUseCase;
    }

    /**
     * period=THIS_MONTH|LAST_MONTH|LAST_30_DAYS ou start/end (até 366 dias). Sem parâmetros: mês atual.
     */
    @GetMapping("/summary")
    public DashboardSummaryOutput summary(
            @RequestParam(required = false) PeriodPreset period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return getDashboardSummaryUseCase.execute(new DashboardQuery(period, start, end));
    }
}
