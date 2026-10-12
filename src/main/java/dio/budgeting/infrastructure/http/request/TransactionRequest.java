package dio.budgeting.infrastructure.http.request;

import java.math.BigDecimal;

/**
 * Corpo de POST e PUT. Enums e datas chegam como texto para que valores inválidos virem erros por campo.
 * amount é recebido como número exato para rejeitar decimais em vez de truncá-los.
 * Campos com valor null contam como ausentes.
 */
public record TransactionRequest(String type,
                                 String description,
                                 BigDecimal amount,
                                 String category,
                                 String occurredAt,
                                 String paymentMethod,
                                 String recurrence,
                                 String status,
                                 String notes) {

    /**
     * Contrato legado da v1: nenhum dos campos novos está presente.
     */
    public boolean isLegacy() {
        return type == null && occurredAt == null && paymentMethod == null && recurrence == null
                && status == null && notes == null;
    }
}
