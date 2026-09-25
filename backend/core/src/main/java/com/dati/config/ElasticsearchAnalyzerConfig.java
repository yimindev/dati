package com.dati.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Analyzer configuration for the semantic search index.
 *
 * <p>Analyzer names are Elasticsearch analyzer types, not plugin-specific: {@code standard}
 * is built into Elasticsearch, while {@code ik_max_word} / {@code ik_smart} require the IK
 * plugin. This keeps IK optional for deployments that do not need Chinese word segmentation.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "dati.elasticsearch")
public class ElasticsearchAnalyzerConfig {

    /** Analyzer type used when indexing documents. */
    private String indexAnalyzer = "standard";

    /** Analyzer type used at search time; blank falls back to {@link #indexAnalyzer}. */
    private String searchAnalyzer;
}
