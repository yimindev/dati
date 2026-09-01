package com.dati.mcp.repository.po;

import com.dati.base.pojo.BasePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;

/**
 * Audit trail for LLM-initiated metadata writes (UPDATE_TABLE_INFO /
 * UPDATE_COLUMN_INFO / UPSERT_TERM). One row per change, written in the same
 * transaction as the metadata update. No admin UI in v1.
 */
@Getter
@Setter
@FieldNameConstants
@Entity
@Table(name = "mcp_metadata_audit_log")
public class McpMetadataAuditLogPO extends BasePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "service_id", length = 64)
    private String serviceId;

    @Column(name = "tool_type", length = 30)
    private String toolType;

    @Column(name = "entity_type", length = 20)
    private String entityType;

    @Column(name = "entity_id", length = 36)
    private String entityId;

    @Column(name = "entity_name", length = 255)
    private String entityName;

    @Column(name = "change_type", length = 10)
    private String changeType;

    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;
}
