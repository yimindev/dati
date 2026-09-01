package com.dati.permission.domain.model;

public enum Permission {
    VIEW, EDIT;

    /** 级别制：EDIT 隐含 VIEW。当前权限是否覆盖所需权限。 */
    public boolean covers(Permission required) {
        return this.ordinal() >= required.ordinal();
    }
}
