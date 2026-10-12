package dio.budgeting.domain;

/**
 * CONFIRMED entra nos valores realizados; PLANNED fica separado como previsto.
 */
public enum EntryStatus {
    CONFIRMED("Realizado"),
    PLANNED("Previsto");

    private final String label;

    EntryStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
