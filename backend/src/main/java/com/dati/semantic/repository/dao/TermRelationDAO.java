package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.TermRelationPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TermRelationDAO extends JpaRepository<TermRelationPO, String> {

    List<TermRelationPO> findByTermId(String termId);

    Optional<TermRelationPO> findByTermIdAndTableIdAndFieldName(String termId, String tableId, String fieldName);

    List<TermRelationPO> findByTermIdAndTableId(String termId, String tableId);

    void deleteByTermId(String termId);

    void deleteByTermIdAndTableId(String termId, String tableId);
}