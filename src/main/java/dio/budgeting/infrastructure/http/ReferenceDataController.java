package dio.budgeting.infrastructure.http;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;
import dio.budgeting.domain.PaymentMethod;
import dio.budgeting.domain.Recurrence;
import dio.budgeting.infrastructure.http.response.ReferenceDataResponse;
import dio.budgeting.infrastructure.http.response.ReferenceDataResponse.CategoryOptionResponse;
import dio.budgeting.infrastructure.http.response.ReferenceDataResponse.FeaturesResponse;
import dio.budgeting.infrastructure.http.response.ReferenceDataResponse.OptionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
public class ReferenceDataController {
    private final FeaturesResponse features;

    public ReferenceDataController(@Value("${app.features.assistant:false}") boolean assistant,
                                   @Value("${app.features.voice:false}") boolean voice) {
        this.features = new FeaturesResponse(assistant, voice);
    }

    @GetMapping("/reference-data")
    public ReferenceDataResponse referenceData() {
        return new ReferenceDataResponse(
                Arrays.stream(EntryType.values()).map(t -> new OptionResponse(t.name(), t.label())).toList(),
                Arrays.stream(Category.values())
                        .map(c -> new CategoryOptionResponse(c.name(), c.type().name(), c.label(), c.description()))
                        .toList(),
                Arrays.stream(PaymentMethod.values()).map(p -> new OptionResponse(p.name(), p.label())).toList(),
                Arrays.stream(Recurrence.values()).map(r -> new OptionResponse(r.name(), r.label())).toList(),
                Arrays.stream(EntryStatus.values()).map(s -> new OptionResponse(s.name(), s.label())).toList(),
                features);
    }
}
