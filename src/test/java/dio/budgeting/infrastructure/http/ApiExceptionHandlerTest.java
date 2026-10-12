package dio.budgeting.infrastructure.http;

import dio.budgeting.application.TransactionNotFoundException;
import dio.budgeting.domain.DomainValidationException;
import dio.budgeting.domain.FieldViolation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void should_returnProblemDetailWithFieldErrors_when_domainValidationFails() {
        var violation = new FieldViolation("amount", "O valor deve ser maior que zero.");

        var response = handler.handleDomainValidation(new DomainValidationException(List.of(violation)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Dados inválidos");
        assertThat(response.getBody().getProperties()).containsEntry("errors", List.of(violation));
    }

    @Test
    void should_returnProblemDetailWithFieldErrors_when_requestIsInvalid() {
        var violation = new FieldViolation("type", "Campo obrigatório.");

        var response = handler.handleInvalidRequest(new InvalidRequestException(List.of(violation)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getProperties()).containsEntry("errors", List.of(violation));
    }

    @Test
    void should_return404ProblemDetail_when_transactionIsMissing() {
        var response = handler.handleNotFound(new TransactionNotFoundException(UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getTitle()).isEqualTo("Lançamento não encontrado");
    }
}
