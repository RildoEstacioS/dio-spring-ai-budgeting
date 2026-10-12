package dio.budgeting.infrastructure.http;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.application.input.TransactionCommand;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;
import dio.budgeting.domain.FieldViolation;
import dio.budgeting.domain.PaymentMethod;
import dio.budgeting.domain.Recurrence;
import dio.budgeting.infrastructure.http.request.TransactionRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Converte o corpo HTTP nos contratos da aplicação, acumulando todos os erros por campo.
 */
final class TransactionRequestMapper {
    private static final String REQUIRED_IN_FULL_CONTRACT =
            "Campo obrigatório quando qualquer campo novo é enviado (contrato completo).";
    private static final String REQUIRED = "Campo obrigatório.";

    private TransactionRequestMapper() {
    }

    static PersistTransactionInput toLegacyInput(TransactionRequest request) {
        List<FieldViolation> violations = new ArrayList<>();
        var category = parseEnum(Category.class, "category", request.category(), "Informe a categoria.", violations);
        var amount = parseAmount(request.amount(), violations);
        throwIfAny(violations);
        return new PersistTransactionInput(request.description(), amount, category);
    }

    /**
     * Contrato completo. Na criação, status é opcional (derivado da data); na edição, é obrigatório.
     */
    static TransactionCommand toCommand(TransactionRequest request, boolean statusRequired) {
        var requiredMessage = statusRequired ? REQUIRED : REQUIRED_IN_FULL_CONTRACT;
        List<FieldViolation> violations = new ArrayList<>();

        var type = parseEnum(EntryType.class, "type", request.type(), requiredMessage, violations);
        var amount = parseAmount(request.amount(), violations);
        var category = parseEnum(Category.class, "category", request.category(), "Informe a categoria.", violations);
        var occurredAt = parseDate(request.occurredAt(), requiredMessage, violations);
        var paymentMethod = parseEnum(PaymentMethod.class, "paymentMethod", request.paymentMethod(), requiredMessage, violations);
        var recurrence = parseEnum(Recurrence.class, "recurrence", request.recurrence(), requiredMessage, violations);
        EntryStatus status = null;
        if (request.status() != null || statusRequired) {
            status = parseEnum(EntryStatus.class, "status", request.status(),
                    "Informe se o lançamento é realizado ou previsto.", violations);
        }

        throwIfAny(violations);
        return new TransactionCommand(type, request.description(), amount, category, occurredAt, paymentMethod,
                recurrence, status, request.notes());
    }

    private static long parseAmount(BigDecimal amount, List<FieldViolation> violations) {
        if (amount == null) {
            violations.add(new FieldViolation("amount", "Informe o valor em centavos."));
            return 0;
        }
        if (amount.scale() != 0) {
            violations.add(new FieldViolation("amount",
                    "O valor deve ser um número inteiro de centavos (ex.: 8000 para R$ 80,00)."));
            return 0;
        }
        try {
            return amount.longValueExact();
        } catch (ArithmeticException e) {
            violations.add(new FieldViolation("amount", "Valor fora do intervalo permitido."));
            return 0;
        }
    }

    private static LocalDate parseDate(String value, String requiredMessage, List<FieldViolation> violations) {
        if (value == null || value.isBlank()) {
            violations.add(new FieldViolation("occurredAt", requiredMessage));
            return null;
        }
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException e) {
            violations.add(new FieldViolation("occurredAt", "Use o formato AAAA-MM-DD."));
            return null;
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String field, String value, String requiredMessage,
                                                   List<FieldViolation> violations) {
        if (value == null || value.isBlank()) {
            violations.add(new FieldViolation(field, requiredMessage));
            return null;
        }
        try {
            return Enum.valueOf(type, value.strip());
        } catch (IllegalArgumentException e) {
            violations.add(new FieldViolation(field, "Valor não reconhecido para este campo."));
            return null;
        }
    }

    private static void throwIfAny(List<FieldViolation> violations) {
        if (!violations.isEmpty()) {
            throw new InvalidRequestException(violations);
        }
    }
}
