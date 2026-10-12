package dio.budgeting.domain;

public enum PaymentMethod {
    PIX("Pix"),
    DEBIT_CARD("Cartão de débito"),
    CREDIT_CARD("Cartão de crédito"),
    CASH("Dinheiro"),
    BANK_TRANSFER("Transferência bancária"),
    OTHER("Outro");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
