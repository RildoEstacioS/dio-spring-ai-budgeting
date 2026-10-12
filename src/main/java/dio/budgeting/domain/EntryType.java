package dio.budgeting.domain;

public enum EntryType {
    INCOME("Receita"),
    EXPENSE("Despesa");

    private final String label;

    EntryType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
