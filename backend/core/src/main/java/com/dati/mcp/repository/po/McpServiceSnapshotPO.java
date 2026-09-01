package com.dati.mcp.repository.po;

import com.dati.base.pojo.BasePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;

@Setter
@Getter
@FieldNameConstants
@Entity
@Table(name = "mcp_service_snapshot")
public class McpServiceSnapshotPO extends BasePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "service_id", nullable = false)
    private String serviceId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "release_note", length = 500)
    private String releaseNote;

    @Column(name = "snapshot_content", columnDefinition = "TEXT", nullable = false)
    private String snapshotContent;

}
