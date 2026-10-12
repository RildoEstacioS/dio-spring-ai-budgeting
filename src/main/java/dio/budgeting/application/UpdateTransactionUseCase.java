package dio.budgeting.application;

import dio.budgeting.application.input.TransactionCommand;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.TransactionDetails;
import dio.budgeting.domain.TransactionId;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

/**
 * Edição explícita, inclusive para confirmar um lançamento previsto. O status é sempre informado.
 */
@Service
public class UpdateTransactionUseCase {
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public UpdateTransactionUseCase(TransactionRepository transactionRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    public TransactionOutput execute(UUID id, TransactionCommand command) {
        var transaction = transactionRepository.findById(new TransactionId(id))
                .orElseThrow(() -> new TransactionNotFoundException(id));

        transaction.update(new TransactionDetails(command.type(), command.description(), command.amount(),
                command.category(), command.occurredAt(), command.paymentMethod(), command.recurrence(),
                command.status(), command.notes()), clock);

        return TransactionOutput.from(transactionRepository.save(transaction));
    }
}
