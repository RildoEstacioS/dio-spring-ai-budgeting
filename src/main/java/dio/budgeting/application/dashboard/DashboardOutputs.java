package dio.budgeting.application.dashboard;

import dio.budgeting.application.output.AmountOutput;
import dio.budgeting.application.output.MoneyTotalOutput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.DateRange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Estruturas de saída do resumo do dashboard.
 */
public final class DashboardOutputs {
    private DashboardOutputs() {
    }

    public record PeriodOutput(LocalDate start, LocalDate end, long days) {
        public static PeriodOutput of(DateRange range) {
            return new PeriodOutput(range.start(), range.end(), range.days());
        }
    }

    /**
     * Variação em relação ao período anterior. deltaPercent é nulo quando o período anterior é zero.
     */
    public record ComparisonOutput(BigDecimal previous,
                                   long previousInCents,
                                   BigDecimal delta,
                                   long deltaInCents,
                                   BigDecimal deltaPercent) {
    }

    public record ComparisonsOutput(ComparisonOutput income, ComparisonOutput expense, ComparisonOutput balance) {
    }

    public record CategoryTotalOutput(String category,
                                      String label,
                                      BigDecimal total,
                                      long totalInCents,
                                      long count,
                                      BigDecimal sharePercent) {
    }

    /**
     * Lançamentos previstos no período, sempre separados do realizado.
     * projectedBalance = saldo realizado + receitas previstas - despesas previstas.
     */
    public record ForecastOutput(MoneyTotalOutput plannedIncome,
                                 MoneyTotalOutput plannedExpense,
                                 AmountOutput projectedBalance,
                                 long plannedCount,
                                 long overduePlannedCount,
                                 List<TransactionOutput> upcoming) {
    }

    /**
     * Destaque calculado localmente, sem IA.
     */
    public record InsightOutput(String kind, String message) {
    }

    public record DashboardSummaryOutput(String preset,
                                         String label,
                                         PeriodOutput period,
                                         PeriodOutput previousPeriod,
                                         MoneyTotalOutput income,
                                         MoneyTotalOutput expense,
                                         AmountOutput balance,
                                         long entryCount,
                                         ComparisonsOutput comparison,
                                         List<CategoryTotalOutput> expensesByCategory,
                                         List<TransactionOutput> topExpenses,
                                         List<TransactionOutput> recentEntries,
                                         ForecastOutput forecast,
                                         List<InsightOutput> insights) {
    }
}
