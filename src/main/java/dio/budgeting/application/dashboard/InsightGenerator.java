package dio.budgeting.application.dashboard;

import dio.budgeting.application.dashboard.DashboardOutputs.InsightOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.ReportingPeriod;
import dio.budgeting.domain.Transaction;

import java.text.Normalizer;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Destaques determinísticos, calculados localmente e sem IA. Considera somente despesas realizadas.
 */
final class InsightGenerator {
    static final int MAX_RECURRENCE_INSIGHTS = 3;
    static final int MIN_INTERVAL_DAYS = 25;
    static final int MAX_INTERVAL_DAYS = 35;
    static final int MAX_VARIATION_PERCENT = 10;

    private InsightGenerator() {
    }

    static List<InsightOutput> generate(ReportingPeriod period, List<Transaction> current, List<Transaction> previous) {
        List<InsightOutput> insights = new ArrayList<>();
        var currentExpenses = confirmedExpenses(current);
        var previousExpenses = confirmedExpenses(previous);
        var currentTotal = sum(currentExpenses);
        var previousTotal = sum(previousExpenses);

        if (currentTotal > 0) {
            insights.add(topCategory(currentExpenses, currentTotal));
            var largest = currentExpenses.stream().sorted(DashboardCalculator.LARGEST_FIRST).findFirst().orElseThrow();
            insights.add(new InsightOutput("LARGEST_EXPENSE", "O maior gasto do período foi \"%s\" em %s: %s."
                    .formatted(largest.getDescription(), BrlFormat.date(largest.getOccurredAt()),
                            BrlFormat.money(largest.getAmount()))));
        }

        var previousRange = "%s a %s".formatted(BrlFormat.date(period.previous().start()), BrlFormat.date(period.previous().end()));
        if (previousTotal > 0) {
            insights.add(comparison(currentTotal, previousTotal, previousRange));
        } else if (currentTotal > 0) {
            insights.add(new InsightOutput("EXPENSE_COMPARISON",
                    "Não há despesas realizadas no período de %s para comparar.".formatted(previousRange)));
        }

        insights.addAll(possibleRecurrences(period, Stream.concat(previousExpenses.stream(), currentExpenses.stream()).toList()));
        return insights;
    }

    private static InsightOutput topCategory(List<Transaction> expenses, long total) {
        Map<Category, Long> byCategory = new EnumMap<>(Category.class);
        for (var t : expenses) {
            byCategory.merge(t.getCategory(), t.getAmount(), Math::addExact);
        }
        var top = byCategory.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<Category, Long> e) -> e.getValue()).reversed()
                        .thenComparing(e -> e.getKey().label()))
                .findFirst().orElseThrow();
        return new InsightOutput("TOP_CATEGORY", "A categoria com maior despesa realizada foi %s: %s (%s das despesas do período)."
                .formatted(top.getKey().label(), BrlFormat.money(top.getValue()),
                        BrlFormat.percent(Percentages.share(top.getValue(), total))));
    }

    private static InsightOutput comparison(long currentTotal, long previousTotal, String previousRange) {
        if (currentTotal == previousTotal) {
            return new InsightOutput("EXPENSE_COMPARISON", "As despesas realizadas ficaram iguais às do período de %s (%s)."
                    .formatted(previousRange, BrlFormat.money(currentTotal)));
        }
        var direction = currentTotal > previousTotal ? "subiram" : "caíram";
        return new InsightOutput("EXPENSE_COMPARISON", "As despesas realizadas %s %s em relação ao período de %s (de %s para %s)."
                .formatted(direction, BrlFormat.percent(Percentages.change(currentTotal, previousTotal)), previousRange,
                        BrlFormat.money(previousTotal), BrlFormat.money(currentTotal)));
    }

    /**
     * Heurística transparente: mesma categoria, descrição normalizada igual, valor dentro de ±10% e intervalo
     * de 25 a 35 dias, com a ocorrência mais recente dentro do período. Ignora lançamentos já marcados como recorrentes.
     */
    private static List<InsightOutput> possibleRecurrences(ReportingPeriod period, List<Transaction> expenses) {
        Map<String, List<Transaction>> groups = new TreeMap<>(expenses.stream()
                .filter(t -> !t.getRecurrence().isRecurring())
                .filter(t -> !normalize(t.getDescription()).isEmpty())
                .collect(Collectors.groupingBy(t -> t.getCategory().name() + "|" + normalize(t.getDescription()))));

        List<InsightOutput> insights = new ArrayList<>();
        for (var group : groups.values()) {
            var ordered = group.stream()
                    .sorted(Comparator.comparing(Transaction::getOccurredAt).thenComparing(t -> t.getId().uuid().toString()))
                    .toList();
            for (int i = ordered.size() - 1; i > 0; i--) {
                var later = ordered.get(i);
                var earlier = ordered.get(i - 1);
                if (period.current().contains(later.getOccurredAt()) && looksRecurring(earlier, later)) {
                    var days = ChronoUnit.DAYS.between(earlier.getOccurredAt(), later.getOccurredAt());
                    insights.add(new InsightOutput("POSSIBLE_RECURRENCE",
                            "Possível cobrança recorrente: \"%s\" (%s), %s em %s e %s em %s, intervalo de %d dias. É uma heurística; confirme antes de marcar como recorrente."
                                    .formatted(later.getDescription(), later.getCategory().label(),
                                            BrlFormat.money(earlier.getAmount()), BrlFormat.date(earlier.getOccurredAt()),
                                            BrlFormat.money(later.getAmount()), BrlFormat.date(later.getOccurredAt()), days)));
                    break;
                }
            }
            if (insights.size() == MAX_RECURRENCE_INSIGHTS) {
                break;
            }
        }
        return insights;
    }

    static boolean looksRecurring(Transaction earlier, Transaction later) {
        var days = ChronoUnit.DAYS.between(earlier.getOccurredAt(), later.getOccurredAt());
        var difference = Math.abs(Math.subtractExact(later.getAmount(), earlier.getAmount()));
        return days >= MIN_INTERVAL_DAYS && days <= MAX_INTERVAL_DAYS
                && Math.multiplyExact(difference, 100L) <= Math.multiplyExact(earlier.getAmount(), (long) MAX_VARIATION_PERCENT);
    }

    static String normalize(String description) {
        return Normalizer.normalize(description, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z]+", " ")
                .trim();
    }

    private static List<Transaction> confirmedExpenses(List<Transaction> transactions) {
        return transactions.stream().filter(Transaction::isConfirmed).filter(Transaction::isExpense).toList();
    }

    private static long sum(List<Transaction> transactions) {
        return transactions.stream().mapToLong(Transaction::getAmount).reduce(0L, Math::addExact);
    }
}
