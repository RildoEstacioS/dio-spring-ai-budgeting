package dio.budgeting.application.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Percentages {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Percentages() {
    }

    /**
     * Participação de part em whole, com 1 casa decimal (HALF_EVEN). Nulo quando whole é zero.
     */
    static BigDecimal share(long part, long whole) {
        if (whole == 0) {
            return null;
        }
        return BigDecimal.valueOf(part).multiply(HUNDRED).divide(BigDecimal.valueOf(whole), 1, RoundingMode.HALF_EVEN);
    }

    /**
     * Variação de previous para current, relativa ao valor absoluto de previous. Nula quando previous é zero.
     */
    static BigDecimal change(long current, long previous) {
        if (previous == 0) {
            return null;
        }
        var delta = Math.subtractExact(current, previous);
        return BigDecimal.valueOf(delta).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(Math.abs(previous)), 1, RoundingMode.HALF_EVEN);
    }
}
