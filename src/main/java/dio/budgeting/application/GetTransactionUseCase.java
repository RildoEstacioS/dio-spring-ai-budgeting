package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.TransactionId;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetTransactionUseCase {
    private final TransactionRepository transactionRepository;

    public GetTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionOutput execute(UUID id) {
        return transactionRepository.findById(new TransactionId(id))
                .map(TransactionOutput::from)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }
}
