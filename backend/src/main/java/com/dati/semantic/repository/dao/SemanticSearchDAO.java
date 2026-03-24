package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SemanticSearchDAO extends ElasticsearchRepository<SemanticSearchDocument, String> {

    void deleteByEntity_TableId(String tableId);
}
