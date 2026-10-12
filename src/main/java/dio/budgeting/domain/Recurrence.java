package dio.budgeting.domain;

/**
 * Característica registrada do lançamento. Não gera lançamentos automaticamente.
 */
public enum Recurrence {
    NONE("Não recorrente"),
    WEEKLY("Semanal"),
    MONTHLY("Mensal"),
    YEARLY("Anual");

    private final String label;

    Recurrence(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isRecurring() {
        return this != NONE;
    }
}
