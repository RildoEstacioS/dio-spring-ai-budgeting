package dio.budgeting.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    @Test
    void should_have5IncomeAnd11ExpenseCategories() {
        var byType = Arrays.stream(Category.values()).collect(Collectors.groupingBy(Category::type, Collectors.counting()));

        assertThat(byType).containsEntry(EntryType.INCOME, 5L).containsEntry(EntryType.EXPENSE, 11L);
    }

    @Test
    void should_keepV1Names_when_evolvingCategories() {
        assertThat(Category.valueOf("GROCERIES").type()).isEqualTo(EntryType.EXPENSE);
        assertThat(Category.valueOf("PHARMA").type()).isEqualTo(EntryType.EXPENSE);
        assertThat(Category.valueOf("AUTO").type()).isEqualTo(EntryType.EXPENSE);
    }

    @Test
    void should_haveUniquePortugueseLabelsAndDescriptions() {
        assertThat(Category.values()).allSatisfy(category -> {
            assertThat(category.label()).isNotBlank();
            assertThat(category.description()).isNotBlank();
        });
        assertThat(Arrays.stream(Category.values()).map(Category::label).distinct()).hasSize(Category.values().length);
    }

    @Test
    void should_checkCompatibility_by_entryType() {
        assertThat(Category.SALARY.isCompatibleWith(EntryType.INCOME)).isTrue();
        assertThat(Category.SALARY.isCompatibleWith(EntryType.EXPENSE)).isFalse();
        assertThat(Category.PHARMA.isCompatibleWith(EntryType.EXPENSE)).isTrue();
    }
}
