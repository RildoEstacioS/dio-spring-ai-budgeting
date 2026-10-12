package dio.budgeting.infrastructure.http;

import dio.budgeting.domain.FieldViolation;

import java.util.List;

/**
 * Corpo ou parâmetros que não puderam ser convertidos para o contrato da API.
 */
public class InvalidRequestException extends RuntimeException {
    private final List<FieldViolation> violations;

    public InvalidRequestException(List<FieldViolation> violations) {
        super("Requisição inválida");
        this.violations = List.copyOf(violations);
    }

    public List<FieldViolation> violations() {
        return violations;
    }
}
