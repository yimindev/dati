package com.dati.semantic.repository;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds Elasticsearch index settings for the semantic search index.
 *
 * <p>The analyzer names are fixed aliases referenced by the {@code @Field} annotations on
 * {@link com.dati.semantic.repository.po.SemanticSearchDocument}; the actual analyzer
 * implementation (e.g. {@code standard} or {@code ik_max_word}) is injected at index
 * creation time so that no Elasticsearch plugin is required to run the application.</p>
 */
public final class SemanticIndexSettings {

    /** Alias referenced by the index-time {@code analyzer} in the field mappings. */
    public static final String INDEX_ANALYZER = "dati_text_analyzer";

    /** Alias referenced by the {@code searchAnalyzer} in the field mappings. */
    public static final String SEARCH_ANALYZER = "dati_search_analyzer";

    /** Number of shards for the semantic search index (previously defined by {@code @Setting(shards = 3)}). */
    public static final int NUMBER_OF_SHARDS = 3;

    private SemanticIndexSettings() {
    }

    /**
     * Builds the analysis settings that define the two alias analyzers.
     *
     * @param indexAnalyzer  analyzer type used at index time (e.g. {@code standard}, {@code ik_max_word})
     * @param searchAnalyzer analyzer type used at search time; blank falls back to {@code indexAnalyzer}
     * @return settings map accepted by {@link org.springframework.data.elasticsearch.core.IndexOperations#create(Map)}
     */
    public static Map<String, Object> build(String indexAnalyzer, String searchAnalyzer) {
        String resolvedSearchAnalyzer = (searchAnalyzer == null || searchAnalyzer.isBlank())
                ? indexAnalyzer
                : searchAnalyzer;

        Map<String, Object> analyzers = new HashMap<>();
        analyzers.put(INDEX_ANALYZER, Map.of("type", indexAnalyzer));
        analyzers.put(SEARCH_ANALYZER, Map.of("type", resolvedSearchAnalyzer));

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("analyzer", analyzers);

        Map<String, Object> settings = new HashMap<>();
        settings.put("number_of_shards", NUMBER_OF_SHARDS);
        settings.put("analysis", analysis);
        return settings;
    }

    /**
     * Reads the analyzers effectively applied to the semantic text fields from the index mapping,
     * resolving alias references through the analyzer definitions in the index settings.
     *
     * @param mapping  index mapping (as returned by {@code IndexOperations#getMapping()})
     * @param settings index settings (as returned by {@code IndexOperations#getSettings()})
     * @return the analyzer types actually applied by the existing index
     */
    public static AppliedAnalyzers appliedAnalyzers(Map<String, Object> mapping, Map<String, Object> settings) {
        Object indexRef = nestedGet(mapping, "properties", "description", "analyzer");
        Object searchRef = nestedGet(mapping, "properties", "description", "search_analyzer");
        String indexAnalyzerName = indexRef != null ? indexRef.toString() : null;
        // Elasticsearch falls back to the field analyzer when search_analyzer is not set
        String searchAnalyzerName = searchRef != null ? searchRef.toString() : indexAnalyzerName;
        return new AppliedAnalyzers(
                indexAnalyzerName != null ? resolveAnalyzerType(settings, indexAnalyzerName) : "standard",
                searchAnalyzerName != null ? resolveAnalyzerType(settings, searchAnalyzerName) : "standard");
    }

    /** Analyzer types effectively applied to the semantic text fields of an existing index. */
    public record AppliedAnalyzers(String indexAnalyzer, String searchAnalyzer) {
    }

    /**
     * Checks whether an existing index mapping describes the expected semantic document structure:
     * a mapped text field and a nested {@code entity} object. Detects indexes left over by a partial
     * creation or created outside the application (e.g. by Elasticsearch dynamic mapping).
     */
    public static boolean hasExpectedMapping(Map<String, Object> mapping) {
        Object description = nestedGet(mapping, "properties", "description");
        Object entityType = nestedGet(mapping, "properties", "entity", "type");
        return description != null && "nested".equals(entityType);
    }

    private static String resolveAnalyzerType(Map<String, Object> settings, String analyzerName) {
        if (INDEX_ANALYZER.equals(analyzerName) || SEARCH_ANALYZER.equals(analyzerName)) {
            Object type = nestedGet(settings, "index", "analysis", "analyzer", analyzerName, "type");
            if (type != null) {
                return type.toString();
            }
        }
        // not an alias: the name in the mapping is the analyzer type itself
        return analyzerName;
    }

    private static Object nestedGet(Map<String, Object> map, Object... keys) {
        Object current = map;
        for (Object key : keys) {
            if (!(current instanceof Map<?, ?> nested)) {
                return null;
            }
            current = nested.get(key);
        }
        return current;
    }
}
