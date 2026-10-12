package dio.budgeting.application.output;

import dio.budgeting.domain.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static dio.budgeting.domain.TestTransactions.TODAY;
import static dio.budgeting.domain.TestTransactions.entry;
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
        var transaction = entry().expense("Farmácia", 8000, Category.PHARMA).notes("Receita do médico").build();

        var output = TransactionOutput.from(transaction);

        assertThat(output.id()).isEqualTo(transaction.getId().uuid().toString());
        assertThat(output.type()).isEqualTo("EXPENSE");
        assertThat(output.description()).isEqualTo("Farmácia");
        assertThat(output.category()).isEqualTo("PHARMA");
        assertThat(output.amount()).isEqualTo(new BigDecimal("80.00"));
        assertThat(output.amountInCents()).isEqualTo(8000);
        assertThat(output.occurredAt()).isEqualTo(TODAY);
        assertThat(output.paymentMethod()).isEqualTo("PIX");
        assertThat(output.recurrence()).isEqualTo("NONE");
        assertThat(output.status()).isEqualTo("CONFIRMED");
        assertThat(output.notes()).isEqualTo("Receita do médico");
        assertThat(output.createdAt()).isEqualTo(transaction.getCreatedAt());
    }
}
