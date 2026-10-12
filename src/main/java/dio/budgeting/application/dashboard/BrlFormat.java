package dio.budgeting.application.dashboard;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatação pt-BR para textos gerados no servidor (destaques). Valores sempre a partir de centavos.
 */
final class BrlFormat {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private BrlFormat() {
    }

    static String money(long cents) {
        return NumberFormat.getCurrencyInstance(PT_BR)
                .format(BigDecimal.valueOf(cents, 2))
                .replace(' ', ' ');
    }

    static String percent(BigDecimal value) {
        return value.abs().toPlainString().replace('.', ',') + "%";
    }

    static String date(LocalDate date) {
        return DATE.format(date);
    }
}
