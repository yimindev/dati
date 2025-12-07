package com.dati.semantic.repository.dao;

import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SemanticSearchRepository extends ElasticsearchRepository<SemanticSearchDocument, String> {
    
    // 根据类型查询
    List<SemanticSearchDocument> findByType(SemanticEntityType type);
    
    // 根据关键词搜索
    List<SemanticSearchDocument> findByKeywordsContaining(String keyword);
}
