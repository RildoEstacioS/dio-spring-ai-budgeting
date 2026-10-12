package dio.budgeting.application.output;

import java.math.BigDecimal;

/**
 * Resumo de gastos devolvido à IA. Valores monetários em reais.
 *
 * @param category         categoria filtrada, ou {@code null} quando o resumo considera todas
 * @param total            soma dos gastos considerados
 * @param transactionCount quantidade de transações consideradas
 * @param largestExpense   maior despesa encontrada, ou {@code null} quando não há transações
 */
public record ExpenseSummaryOutput(String category,
                                   BigDecimal total,
                                   int transactionCount,
                                   TransactionOutput largestExpense) {
}
