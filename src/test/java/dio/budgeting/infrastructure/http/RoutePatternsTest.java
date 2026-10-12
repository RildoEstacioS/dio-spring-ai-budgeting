package dio.budgeting.infrastructure.http;

import dio.budgeting.domain.Category;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class RoutePatternsTest {

    @Test
    void should_listExactlyTheCategoriesOfTheEnum() {
        assertThat(RoutePatterns.CATEGORY.split("\\|"))
                .containsExactlyInAnyOrder(Arrays.stream(Category.values()).map(Enum::name).toArray(String[]::new));
    }

    @Test
    void should_matchOnlyRealCategories() {
        var pattern = Pattern.compile(RoutePatterns.CATEGORY);

        assertThat(pattern.matcher("PHARMA").matches()).isTrue();
        assertThat(pattern.matcher("OTHER_EXPENSE").matches()).isTrue();
        assertThat(pattern.matcher("pharma").matches()).isFalse();
        assertThat(pattern.matcher("ANYTHING").matches()).isFalse();
    }

    @Test
    void should_matchOnlyCompleteUuids() {
        var pattern = Pattern.compile(RoutePatterns.UUID);
        var uuid = UUID.randomUUID().toString();

        assertThat(pattern.matcher(uuid).matches()).isTrue();
        assertThat(pattern.matcher(uuid.toUpperCase()).matches()).isTrue();
        assertThat(pattern.matcher(uuid.replace("-", "")).matches()).isFalse();
        assertThat(pattern.matcher(uuid.substring(1)).matches()).isFalse();
        assertThat(pattern.matcher("PHARMA").matches()).isFalse();
    }
}
