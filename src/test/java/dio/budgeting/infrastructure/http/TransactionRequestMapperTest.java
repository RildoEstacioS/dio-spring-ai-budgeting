package dio.budgeting.infrastructure.http;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;
import dio.budgeting.domain.FieldViolation;
import dio.budgeting.domain.PaymentMethod;
import dio.budgeting.domain.Recurrence;
import dio.budgeting.infrastructure.http.request.TransactionRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionRequestMapperTest {

    @Test
    void should_detectLegacyContract_when_noNewFieldIsPresent() {
        assertThat(legacy("Farmácia", new BigDecimal("8000"), "PHARMA").isLegacy()).isTrue();
        assertThat(new TransactionRequest(null, "Farmácia", new BigDecimal("8000"), "PHARMA", null, null, null, null, "obs")
                .isLegacy()).isFalse();
    }

    @Test
    void should_mapLegacyInput_when_valuesAreValid() {
        var input = TransactionRequestMapper.toLegacyInput(legacy("Farmácia", new BigDecimal("8000"), "PHARMA"));

        assertThat(input.description()).isEqualTo("Farmácia");
        assertThat(input.amount()).isEqualTo(8000);
        assertThat(input.category()).isEqualTo(Category.PHARMA);
    }

    @Test
    void should_rejectAmount_when_notAnIntegerNumberOfCents() {
        assertThat(fields(() -> TransactionRequestMapper.toLegacyInput(legacy("X", new BigDecimal("80.5"), "PHARMA"))))
                .containsExactly("amount");
        assertThat(fields(() -> TransactionRequestMapper.toLegacyInput(legacy("X", new BigDecimal("8000.0"), "PHARMA"))))
                .containsExactly("amount");
        assertThat(fields(() -> TransactionRequestMapper.toLegacyInput(legacy("X", new BigDecimal("8E3"), "PHARMA"))))
                .containsExactly("amount");
        assertThat(fields(() -> TransactionRequestMapper.toLegacyInput(legacy("X", null, "PHARMA"))))
                .containsExactly("amount");
        assertThat(fields(() -> TransactionRequestMapper.toLegacyInput(legacy("X", new BigDecimal("99999999999999999999"), "PHARMA"))))
                .containsExactly("amount");
    }

    @Test
    void should_rejectUnknownCategory() {
        assertThat(fields(() -> TransactionRequestMapper.toLegacyInput(legacy("X", new BigDecimal("100"), "pharma"))))
                .containsExactly("category");
    }

    @Test
    void should_requireCoreFields_when_anyNewFieldIsPresent() {
        var partial = new TransactionRequest(null, "Farmácia", new BigDecimal("8000"), "PHARMA", null, null, null, null, "obs");

        var violations = violations(() -> TransactionRequestMapper.toCommand(partial, false));

        assertThat(violations).extracting(FieldViolation::field)
                .containsExactly("type", "occurredAt", "paymentMethod", "recurrence");
        assertThat(violations.getFirst().message()).contains("contrato completo");
    }

    @Test
    void should_mapFullContract_with_optionalStatus() {
        var command = TransactionRequestMapper.toCommand(full(null, "2026-10-12"), false);

        assertThat(command.type()).isEqualTo(EntryType.INCOME);
        assertThat(command.amount()).isEqualTo(650000);
        assertThat(command.category()).isEqualTo(Category.SALARY);
        assertThat(command.occurredAt()).isEqualTo(LocalDate.parse("2026-10-12"));
        assertThat(command.paymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
        assertThat(command.recurrence()).isEqualTo(Recurrence.MONTHLY);
        assertThat(command.status()).isNull();
    }

    @Test
    void should_requireStatus_when_updating() {
        assertThat(fields(() -> TransactionRequestMapper.toCommand(full(null, "2026-10-12"), true))).containsExactly("status");
        assertThat(TransactionRequestMapper.toCommand(full("PLANNED", "2026-10-12"), true).status()).isEqualTo(EntryStatus.PLANNED);
    }

    @Test
    void should_rejectInvalidDateAndEnumValues() {
        var request = new TransactionRequest("RECEITA", "Salário", new BigDecimal("650000"), "SALARY", "12/10/2026",
                "PIX", "MONTHLY", "DONE", null);

        assertThat(fields(() -> TransactionRequestMapper.toCommand(request, false)))
                .containsExactly("type", "occurredAt", "status");
    }

    private static TransactionRequest legacy(String description, BigDecimal amount, String category) {
        return new TransactionRequest(null, description, amount, category, null, null, null, null, null);
    }

    private static TransactionRequest full(String status, String date) {
        return new TransactionRequest("INCOME", "Salário", new BigDecimal("650000"), "SALARY", date, "BANK_TRANSFER",
                "MONTHLY", status, null);
    }

    private static List<String> fields(Runnable action) {
        return violations(action).stream().map(FieldViolation::field).toList();
    }

    private static List<FieldViolation> violations(Runnable action) {
        try {
            action.run();
        } catch (InvalidRequestException e) {
            return e.violations();
        }
        throw new AssertionError("Esperava InvalidRequestException");
    }
}
