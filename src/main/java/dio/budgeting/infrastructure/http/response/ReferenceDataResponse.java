package dio.budgeting.infrastructure.http.response;

import java.util.List;

/**
 * Opções usadas pela interface (rótulos em pt-BR) e recursos habilitados no servidor.
 */
public record ReferenceDataResponse(List<OptionResponse> types,
                                    List<CategoryOptionResponse> categories,
                                    List<OptionResponse> paymentMethods,
                                    List<OptionResponse> recurrences,
                                    List<OptionResponse> statuses,
                                    FeaturesResponse features) {

    public record OptionResponse(String value, String label) {
    }

    public record CategoryOptionResponse(String value, String type, String label, String description) {
    }

    public record FeaturesResponse(boolean assistant, boolean voice) {
    }
}
