package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.application.input.TransactionCommand;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;
import dio.budgeting.domain.PaymentMethod;
import dio.budgeting.domain.Recurrence;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionDetails;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class PersistTransactionUseCase {
    /**
     * Campos preenchidos automaticamente no contrato legado (compatibilidade temporária com a v1).
     */
    public static final List<String> LEGACY_DEFAULT_FIELDS =
            List.of("type", "occurredAt", "paymentMethod", "recurrence", "status");

    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public PersistTransactionUseCase(TransactionRepository transactionRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    /**
     * Contrato legado (v1 e tool de voz): despesa realizada hoje, sem meio de pagamento informado.
     */
    @Tool(name = "persist-transaction", description = "Persiste uma nova transação financeira")
    public TransactionOutput execute(PersistTransactionInput input) {
        return create(new TransactionCommand(
                EntryType.EXPENSE,
                input.description(),
                input.amount(),
                input.category(),
                LocalDate.now(clock),
                PaymentMethod.OTHER,
                Recurrence.NONE,
                EntryStatus.CONFIRMED,
                null));
    }

    /**
     * Contrato completo. Sem status, datas futuras viram previstas e as demais, realizadas.
     */
    public TransactionOutput create(TransactionCommand command) {
        var status = command.status() != null ? command.status() : deriveStatus(command.occurredAt());
        var details = new TransactionDetails(command.type(), command.description(), command.amount(),
                command.category(), command.occurredAt(), command.paymentMethod(), command.recurrence(),
                status, command.notes());

        var transaction = transactionRepository.save(Transaction.create(details, clock));
        return TransactionOutput.from(transaction);
    }

    private EntryStatus deriveStatus(LocalDate occurredAt) {
        return occurredAt != null && occurredAt.isAfter(LocalDate.now(clock))
                ? EntryStatus.PLANNED
                : EntryStatus.CONFIRMED;
    }
}
