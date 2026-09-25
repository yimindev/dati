package com.dati.semantic.repository;

import com.dati.config.ElasticsearchAnalyzerConfig;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Creates the semantic search index with the configured analyzers if it does not exist yet.
 *
 * <p>Index creation is taken over from Spring Data Elasticsearch ({@code @Document(createIndex = false)}
 * on {@link SemanticSearchDocument}) because the analyzer implementation depends on runtime
 * configuration ({@link ElasticsearchAnalyzerConfig}) and cannot be expressed in annotations.</p>
 */
@Slf4j
@Component
public class SemanticIndexInitializer {

    private final ObjectProvider<ElasticsearchOperations> operationsProvider;
    private final ElasticsearchAnalyzerConfig analyzerConfig;

    public SemanticIndexInitializer(ObjectProvider<ElasticsearchOperations> operationsProvider,
                                    ElasticsearchAnalyzerConfig analyzerConfig) {
        this.operationsProvider = operationsProvider;
        this.analyzerConfig = analyzerConfig;
    }

    @PostConstruct
    void initIndex() {
        ElasticsearchOperations operations = operationsProvider.getIfAvailable();
        if (operations == null) {
            log.debug("Elasticsearch is not configured, skipping semantic search index initialization");
            return;
        }

        IndexOperations indexOps = operations.indexOps(SemanticSearchDocument.class);
        String indexName = indexOps.getIndexCoordinates().getIndexName();
        try {
            if (indexOps.exists()) {
                reportExistingIndexState(indexOps, indexName);
                return;
            }
            // Settings and mapping are sent in a single atomic create-index request,
            // so a failure cannot leave behind an index without its mapping.
            indexOps.create(SemanticIndexSettings.build(analyzerConfig.getIndexAnalyzer(),
                    analyzerConfig.getSearchAnalyzer()), indexOps.createMapping());
            log.info("Created semantic search index [{}]: index analyzer [{}], search analyzer [{}]",
                    indexName, analyzerConfig.getIndexAnalyzer(), resolvedSearchAnalyzer());
        } catch (RuntimeException e) {
            log.error("Failed to create semantic search index [{}] with index analyzer [{}] and search analyzer [{}]. "
                            + "If the analyzer is not available in Elasticsearch (ik_max_word/ik_smart require the IK plugin), "
                            + "install the plugin or switch to a built-in analyzer via DATI_ELASTICSEARCH_INDEX_ANALYZER / "
                            + "DATI_ELASTICSEARCH_SEARCH_ANALYZER (e.g. standard).",
                    indexName, analyzerConfig.getIndexAnalyzer(), resolvedSearchAnalyzer());
            throw e;
        }
    }

    private void reportExistingIndexState(IndexOperations indexOps, String indexName) {
        try {
            Map<String, Object> mapping = indexOps.getMapping();
            if (!SemanticIndexSettings.hasExpectedMapping(mapping)) {
                log.warn("Semantic search index [{}] already exists but its mapping does not match the expected "
                                + "semantic document structure (it may have been created outside the application or "
                                + "left over by a partial creation). Delete the index and restart the application "
                                + "to rebuild it (data is rebuilt on the next sync).", indexName);
                return;
            }
            SemanticIndexSettings.AppliedAnalyzers applied =
                    SemanticIndexSettings.appliedAnalyzers(mapping, indexOps.getSettings());
            String configuredIndex = analyzerConfig.getIndexAnalyzer();
            String configuredSearch = resolvedSearchAnalyzer();
            if (configuredIndex.equals(applied.indexAnalyzer()) && configuredSearch.equals(applied.searchAnalyzer())) {
                log.info("Semantic search index [{}] already exists, analyzers match the configuration: index [{}], search [{}]",
                        indexName, applied.indexAnalyzer(), applied.searchAnalyzer());
            } else {
                log.warn("Semantic search index [{}] already exists with different analyzers: index [{}], search [{}] "
                                + "(configured: index [{}], search [{}]). The analyzer configuration only applies when the "
                                + "index is created; to apply it, delete the index and restart the application "
                                + "(data is rebuilt on the next sync).",
                        indexName, applied.indexAnalyzer(), applied.searchAnalyzer(), configuredIndex, configuredSearch);
            }
        } catch (RuntimeException e) {
            log.warn("Semantic search index [{}] already exists, but its analyzers could not be verified "
                    + "against the configuration: {}", indexName, e.getMessage());
        }
    }

    private String resolvedSearchAnalyzer() {
        String searchAnalyzer = analyzerConfig.getSearchAnalyzer();
        return (searchAnalyzer == null || searchAnalyzer.isBlank())
                ? analyzerConfig.getIndexAnalyzer()
                : searchAnalyzer;
    }
}
