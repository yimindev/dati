package com.dati.datasource.repository.mapper;

import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.repository.po.ColumnInfoPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ColumnMapper 单元测试")
class ColumnMapperTest {

    @Test
    @DisplayName("测试 extractValueEnabled 字段转换 - PO 到 Model")
    void testColumnInfoExtractValueFields() {
        ColumnInfoPO po = new ColumnInfoPO();
        po.setExtractValueEnabled(true);

        ColumnInfo info = ColumnMapper.toColumnInfo(po);
        assertNotNull(info);
        assertTrue(info.getExtractValueEnabled());
    }

    @Test
    @DisplayName("测试 extractValueEnabled 字段转换 - Model 到 PO")
    void testColumnInfoPOExtractValueFields() {
        ColumnInfo info = new ColumnInfo();
        info.setExtractValueEnabled(true);

        ColumnInfoPO po = ColumnMapper.toColumnInfoPO(info);
        assertNotNull(po);
        assertTrue(po.isExtractValueEnabled());
    }

    @Test
    @DisplayName("测试默认值转换 - PO 到 Model")
    void testDefaultValuesToModel() {
        ColumnInfoPO po = new ColumnInfoPO();

        ColumnInfo info = ColumnMapper.toColumnInfo(po);
        assertNotNull(info);
        assertNotNull(info.getExtractValueEnabled());
        assertEquals(false, info.getExtractValueEnabled());
    }

    @Test
    @DisplayName("测试默认值转换 - Model 到 PO")
    void testDefaultValuesToPO() {
        ColumnInfo info = new ColumnInfo();

        ColumnInfoPO po = ColumnMapper.toColumnInfoPO(info);
        assertNotNull(po);
        assertFalse(po.isExtractValueEnabled());
    }
}
