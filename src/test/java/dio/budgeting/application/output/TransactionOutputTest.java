package dio.budgeting.application.output;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionOutputTest {

    @ParameterizedTest
    @CsvSource({
            "8000, 80.00",
            "1999, 19.99",
            "5, 0.05",
            "0, 0.00",
    })
    void should_convertCentsToReais_when_amountIsInCents(long amountInCents, String expectedReais) {
        var reais = TransactionOutput.centsToReais(amountInCents);

        assertThat(reais).isEqualTo(new BigDecimal(expectedReais));
    }

    @Test
    void should_mapAllFields_when_transactionIsConverted() {
        var transaction = new Transaction("Farmácia", 8000, Category.PHARMA);

        var output = TransactionOutput.from(transaction);

        assertThat(output.id()).isEqualTo(transaction.getId().uuid().toString());
        assertThat(output.description()).isEqualTo("Farmácia");
        assertThat(output.category()).isEqualTo("PHARMA");
        assertThat(output.value()).isEqualTo(new BigDecimal("80.00"));
    }
}
