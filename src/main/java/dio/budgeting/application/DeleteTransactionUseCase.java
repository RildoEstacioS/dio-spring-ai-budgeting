package dio.budgeting.application;

import dio.budgeting.domain.TransactionId;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeleteTransactionUseCase {
    private final TransactionRepository transactionRepository;

    public DeleteTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void execute(UUID id) {
        var transactionId = new TransactionId(id);
        if (transactionRepository.findById(transactionId).isEmpty()) {
            throw new TransactionNotFoundException(id);
        }
        transactionRepository.deleteById(transactionId);
    }
}
