package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.TermPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TermDAO extends JpaRepository<TermPO, String> {

    List<TermPO> findBySubjectId(String subjectId);
}