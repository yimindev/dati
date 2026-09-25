package com.dati.semantic.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SemanticIndexSettings")
class SemanticIndexSettingsTest {

    @Test
    @DisplayName("should build settings with standard analyzers by default")
    void shouldBuildSettingsWithStandardAnalyzersByDefault() {
        Map<String, Object> settings = SemanticIndexSettings.build("standard", null);

        assertEquals("standard", analyzerType(settings, SemanticIndexSettings.INDEX_ANALYZER));
        assertEquals("standard", analyzerType(settings, SemanticIndexSettings.SEARCH_ANALYZER));
        assertEquals(3, settings.get("number_of_shards"));
    }

    @Test
    @DisplayName("should use configured analyzers when both are provided")
    void shouldUseConfiguredAnalyzersWhenBothProvided() {
        Map<String, Object> settings = SemanticIndexSettings.build("ik_max_word", "ik_smart");

        assertEquals("ik_max_word", analyzerType(settings, SemanticIndexSettings.INDEX_ANALYZER));
        assertEquals("ik_smart", analyzerType(settings, SemanticIndexSettings.SEARCH_ANALYZER));
    }

    @Test
    @DisplayName("should fall back to index analyzer when search analyzer is blank")
    void shouldFallBackToIndexAnalyzerWhenSearchAnalyzerBlank() {
        Map<String, Object> settings = SemanticIndexSettings.build("standard", " ");

        assertEquals("standard", analyzerType(settings, SemanticIndexSettings.SEARCH_ANALYZER));
    }

    @Test
    @DisplayName("should resolve aliased analyzers to their types via index settings")
    void shouldResolveAliasedAnalyzersViaIndexSettings() {
        Map<String, Object> mapping = Map.of("properties", Map.of("description", Map.of(
                "type", "text",
                "analyzer", SemanticIndexSettings.INDEX_ANALYZER,
                "search_analyzer", SemanticIndexSettings.SEARCH_ANALYZER)));
        Map<String, Object> settings = Map.of("index", Map.of("analysis", Map.of("analyzer", Map.of(
                SemanticIndexSettings.INDEX_ANALYZER, Map.of("type", "ik_max_word"),
                SemanticIndexSettings.SEARCH_ANALYZER, Map.of("type", "ik_smart")))));

        SemanticIndexSettings.AppliedAnalyzers applied = SemanticIndexSettings.appliedAnalyzers(mapping, settings);

        assertEquals("ik_max_word", applied.indexAnalyzer());
        assertEquals("ik_smart", applied.searchAnalyzer());
    }

    @Test
    @DisplayName("should report concrete analyzers as-is")
    void shouldReportConcreteAnalyzersAsIs() {
        Map<String, Object> mapping = Map.of("properties", Map.of("description", Map.of(
                "type", "text", "analyzer", "ik_max_word", "search_analyzer", "ik_smart")));

        SemanticIndexSettings.AppliedAnalyzers applied = SemanticIndexSettings.appliedAnalyzers(mapping, Map.of());

        assertEquals("ik_max_word", applied.indexAnalyzer());
        assertEquals("ik_smart", applied.searchAnalyzer());
    }

    @Test
    @DisplayName("should report standard analyzers for a dynamically mapped index")
    void shouldReportStandardAnalyzersForDynamicallyMappedIndex() {
        Map<String, Object> mapping = Map.of("properties", Map.of("description", Map.of("type", "text")));

        SemanticIndexSettings.AppliedAnalyzers applied = SemanticIndexSettings.appliedAnalyzers(mapping, Map.of());

        assertEquals("standard", applied.indexAnalyzer());
        assertEquals("standard", applied.searchAnalyzer());
    }

    @Test
    @DisplayName("should fall back to the field analyzer when search analyzer is absent")
    void shouldFallBackToFieldAnalyzerWhenSearchAnalyzerAbsent() {
        Map<String, Object> mapping = Map.of("properties", Map.of("description", Map.of(
                "type", "text", "analyzer", "whitespace")));

        SemanticIndexSettings.AppliedAnalyzers applied = SemanticIndexSettings.appliedAnalyzers(mapping, Map.of());

        assertEquals("whitespace", applied.indexAnalyzer());
        assertEquals("whitespace", applied.searchAnalyzer());
    }

    @Test
    @DisplayName("should accept mapping with text field and nested entity")
    void shouldAcceptMappingWithTextFieldAndNestedEntity() {
        Map<String, Object> mapping = Map.of("properties", Map.of(
                "description", Map.of("type", "text"),
                "entity", Map.of("type", "nested")));

        assertTrue(SemanticIndexSettings.hasExpectedMapping(mapping));
    }

    @Test
    @DisplayName("should reject empty mapping")
    void shouldRejectEmptyMapping() {
        assertFalse(SemanticIndexSettings.hasExpectedMapping(Map.of()));
    }

    @Test
    @DisplayName("should reject mapping with plain object entity")
    void shouldRejectMappingWithPlainObjectEntity() {
        Map<String, Object> mapping = Map.of("properties", Map.of(
                "description", Map.of("type", "text"),
                "entity", Map.of("properties", Map.of("tableId", Map.of("type", "keyword")))));

        assertFalse(SemanticIndexSettings.hasExpectedMapping(mapping));
    }

    @Test
    @DisplayName("should reject mapping without entity field")
    void shouldRejectMappingWithoutEntityField() {
        Map<String, Object> mapping = Map.of("properties", Map.of(
                "description", Map.of("type", "text")));

        assertFalse(SemanticIndexSettings.hasExpectedMapping(mapping));
    }

    @SuppressWarnings("unchecked")
    private static String analyzerType(Map<String, Object> settings, String analyzerName) {
        Map<String, Object> analysis = (Map<String, Object>) settings.get("analysis");
        assertNotNull(analysis);
        Map<String, Object> analyzers = (Map<String, Object>) analysis.get("analyzer");
        assertNotNull(analyzers);
        Map<String, Object> analyzer = (Map<String, Object>) analyzers.get(analyzerName);
        assertNotNull(analyzer);
        return (String) analyzer.get("type");
    }
}
