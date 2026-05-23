package com.dati.mcp.domain.model;

import lombok.Data;

@Data
public class SqlPolicy {
    private boolean allowSelect = true;
    private boolean allowInsert = false;
    private boolean allowUpdate = false;
    private boolean allowDelete = false;
    private boolean allowDdl = false;
    private boolean allowMulti = false;
}
