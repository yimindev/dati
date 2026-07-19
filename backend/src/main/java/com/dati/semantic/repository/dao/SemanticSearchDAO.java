package com.dati.semantic.repository.dao;

import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SemanticSearchDAO extends ElasticsearchRepository<SemanticSearchDocument, String> {

    void deleteByEntity_TableIdIn(List<String> tableIds);

    void deleteByEntity_SubjectId(String subjectId);

    List<SemanticSearchDocument> findByEntity_TableIdAndEntity_FieldAndType(String tableId, String field, SemanticEntityType type);

    void deleteByEntity_TableIdAndEntity_FieldAndType(String tableId, String field, SemanticEntityType type);

    @Query("""
        {
          "bool": {
            "must": [
              { "nested": {
                  "path": "entity",
                  "query": {
                    "term": { "entity.tableId": "?0" }
                  }
              }},
              { "term": { "type": "?1" } }
            ]
          }
        }
        """)
    Page<SemanticSearchDocument> findByEntity_TableIdAndType(String tableId, String type, Pageable pageable);

    @Query("""
        {
          "bool": {
            "must": [
              { "nested": {
                  "path": "entity",
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "entity.tableId": "?0" } },
                        { "term": { "entity.field": "?1" } }
                      ]
                    }
                  }
              }},
              { "term": { "type": "?3" } },
              { "match_bool_prefix": {
                  "keywords": "?2"
              }}
            ]
          }
        }
        """)
    Page<SemanticSearchDocument> searchByTableFieldAndKeyword(
            String tableId, String field, String keyword, String type, Pageable pageable);

    @Query("""
        {
          "bool": {
            "must": [
              { "multi_match": {
                  "query": "?0",
                  "fields": ["keywords", "description"]
              }}
            ],
            "filter": [
              { "bool": {
                  "should": [
                    { "nested": {
                        "path": "entity",
                        "query": { "terms": { "entity.datasourceId": ?1 } }
                    }},
                    { "nested": {
                        "path": "entity",
                        "query": { "terms": { "entity.subjectId": ?2 } }
                    }}
                  ],
                  "minimum_should_match": 1
              }}
            ]
          }
        }
        """)
    Page<SemanticSearchDocument> searchByKeyword(
            String keyword,
            List<String> datasourceIds,
            List<String> subjectIds,
            Pageable pageable);
}
