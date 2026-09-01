package com.dati.db.analysis;

import java.util.List;
import java.util.Set;

public record SqlAnalysisResult(
        SqlOperationType type,
        List<SqlOperationType> statementTypes,
        Set<TableRef> tables) {

    public boolean isMulti() {
        return type == SqlOperationType.MULTI;
    }
}
