package dio.budgeting.application;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(UUID id) {
        super("Lançamento não encontrado: " + id);
    }
}
