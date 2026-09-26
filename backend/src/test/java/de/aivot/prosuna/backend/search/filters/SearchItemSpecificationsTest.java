package de.aivot.prosuna.backend.search.filters;

import de.aivot.prosuna.backend.search.entities.SearchItemEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:search-item-specifications",
        "spring.jpa.properties.hibernate.connection.username=sa",
        "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"
})
@ContextConfiguration(classes = SearchItemSpecificationsTest.Config.class)
public class SearchItemSpecificationsTest {
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        sql("create alias if not exists word_similarity for 'de.aivot.prosuna.backend.search.filters.SearchItemSpecificationsTest.wordSimilarity'");
        sql("drop table if exists v_search_items");
        sql("create table v_search_items (id varchar, origin_table varchar, search_text varchar)");
        insert("instance", "process_instances", "7K0M-9X1Q-4R8T AZ-2026/42 Weitere Aktenzeichen 7K0M9X1Q4R8T");
        insert("uuid", "process_instances", "01234567-89ab-4def-8123-456789abcdef AZ-Alt");
        insert("custom", "process_instances", "Mein-Vorgang/2026 OIL-42");
    }

    @ParameterizedTest
    @ValueSource(strings = {"7K0M-9X1Q-4R8T", "7k0m-9x1q-4r8t", "7K0M9X1Q4R8T", "7kom-9xiq-4r8t",
            "7KOM 9XLQ 4R8T", " 7KOM\u00a09XLQ\u00a04R8T ", "M-9X1", "M9XI"})
    void findsCrockfordSpellingsAndFragmentsOnceWithoutRelyingOnSimilarity(String search) {
        assertEquals(List.of("instance"), find(search, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"01234567-89AB-4DEF-8123-456789ABCDEF", "89ab-4def"})
    void retainsLiteralUuidSearch(String search) {
        assertEquals(List.of("uuid"), find(search, null));
    }

    @Test
    void retainsCustomCaseNumbersAndFileNumbers() {
        assertEquals(List.of("custom"), find("mein-vorgang/2026", null));
        assertEquals(List.of("custom"), find("OIL-42", null));
        assertEquals(List.of("instance"), find("az-2026/42", null));
    }

    @Test
    void doesNotTreatLiteralPercentUnderscoreOrBackslashAsWildcards() {
        insert("literal", "process_instances", "AZ-100%_literal\\part");
        insert("other", "process_instances", "AZ-100XYliteralZpart");

        assertEquals(List.of("literal"), find("100%_literal\\part", null));
    }

    @Test
    void keepsCrockfordNormalizationScopedToInstancesAndHonorsTheOriginFilter() {
        insert("asset", "assets", "7K0M9X1Q4R8T");

        assertEquals(List.of("instance"), find("7KOM9XLQ4R8T", null));
        assertEquals(List.of("asset"), find("7K0M9X1Q4R8T", "assets"));
        assertEquals(List.of("instance"), find("7K0M9X1Q4R8T", "process_instances"));
    }

    @Test
    void prioritizesDirectMatchesOverSimilarityAndKeepsStableOrderingForTies() {
        insert("fuzzy", "assets", "fuzzy candidate");
        insert("b", "process_instances", "contains needle");
        insert("a", "process_instances", "contains needle");

        assertEquals(List.of("a", "b", "fuzzy"), find("needle", null));
    }

    @Test
    void countsTheSameMatchesWithoutAnOrderByClause() {
        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(Long.class);
        var root = query.from(SearchItemEntity.class);
        query.select(builder.count(root));
        query.where(SearchItemSpecifications.matchesSearch("7KOM9XLQ4R8T", null).toPredicate(root, query, builder));

        assertEquals(1L, entityManager.createQuery(query).getSingleResult());
        assertEquals(List.of(), query.getOrderList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void doesNotTurnAnEmptySearchIntoAMatchForEveryRow(String search) {
        assertEquals(List.of(), find(search, null));
    }

    // H2 has no pg_trgm. Stub its score to test the surrounding query's normalization, matching and
    // ordering independently. The sole fuzzy fixture also verifies search-term-first argument order.
    public static double wordSimilarity(String search, String text) {
        return "needle".equals(search) && "fuzzy candidate".equals(text) ? 0.9 : 0.0;
    }

    private List<String> find(String search, String originTable) {
        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(String.class);
        var root = query.from(SearchItemEntity.class);
        query.select(root.get("id"));
        query.where(SearchItemSpecifications.matchesSearch(search, originTable).toPredicate(root, query, builder));
        return entityManager.createQuery(query).getResultList();
    }

    private void insert(String id, String originTable, String searchText) {
        entityManager.createNativeQuery("insert into v_search_items values (:id, :origin, :text)")
                .setParameter("id", id).setParameter("origin", originTable).setParameter("text", searchText).executeUpdate();
    }

    private void sql(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    @Configuration(proxyBeanMethods = false)
    @AutoConfigurationPackage
    @EntityScan(basePackageClasses = SearchItemEntity.class)
    static class Config {
    }
}
