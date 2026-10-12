package dio.budgeting.domain;

/**
 * Regra violada em um campo, com mensagem pronta para o usuário.
 */
public record FieldViolation(String field, String message) {
}
