package dio.budgeting.domain;

import java.util.List;
import java.util.stream.Collectors;

public class DomainValidationException extends RuntimeException {
    private final List<FieldViolation> violations;

    public DomainValidationException(List<FieldViolation> violations) {
        super(violations.stream().map(v -> v.field() + ": " + v.message()).collect(Collectors.joining("; ")));
        this.violations = List.copyOf(violations);
    }

    public List<FieldViolation> violations() {
        return violations;
    }
}
