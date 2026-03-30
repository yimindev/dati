package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SemanticSearchDAO extends ElasticsearchRepository<SemanticSearchDocument, String> {

    void deleteByEntity_TableIdIn(List<String> tableIds);

    void deleteByEntity_SubjectId(String subjectId);
}
