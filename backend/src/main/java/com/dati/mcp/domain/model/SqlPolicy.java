package com.dati.mcp.domain.model;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.db.analysis.SqlAnalysisResult;
import com.dati.db.analysis.SqlOperationType;
import lombok.Data;

@Data
public class SqlPolicy {
    private boolean allowSelect = true;
    private boolean allowInsert = false;
    private boolean allowUpdate = false;
    private boolean allowDelete = false;
    private boolean allowDdl = false;
    private boolean allowMulti = false;
    private boolean allowMetadata = false;
    private boolean allowTransaction = false;
    private boolean allowSet = false;

    /** 逐条类型许可。MERGE 需 INSERT+UPDATE 复合许可。 */
    private boolean allows(SqlOperationType type) {
        return switch (type) {
            case SELECT      -> allowSelect;
            case INSERT      -> allowInsert;
            case UPDATE      -> allowUpdate;
            case DELETE      -> allowDelete;
            case MERGE       -> allowInsert && allowUpdate;
            case DDL         -> allowDdl;
            case METADATA    -> allowMetadata;
            case TRANSACTION -> allowTransaction;
            case SET         -> allowSet;
            case OTHER       -> false;
            case MULTI       -> throw new IllegalArgumentException(
                "MULTI is a marker type; check allowMulti on the result instead");
        };
    }

    /** 根据分析结果校验权限，不通过则抛 DatiException。 */
    public void validateAllowed(SqlAnalysisResult result) {
        if (result.isMulti() && !allowMulti) {
            throw new DatiException(ErrorCode.MS_SQL_POLICY_VIOLATION, "multi-statement not allowed");
        }
        for (SqlOperationType type : result.statementTypes()) {
            if (!allows(type)) {
                throw new DatiException(ErrorCode.MS_SQL_POLICY_VIOLATION, type + " not allowed");
            }
        }
    }
}
