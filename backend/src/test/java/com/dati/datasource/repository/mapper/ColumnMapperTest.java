package com.dati.datasource.repository.mapper;

import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.repository.po.ColumnInfoPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ColumnMapper unit tests")
class ColumnMapperTest {

    @Test
    @DisplayName("extractValueEnabled field conversion - PO to Model")
    void testColumnInfoExtractValueFields() {
        ColumnInfoPO po = new ColumnInfoPO();
        po.setExtractValueEnabled(true);

        ColumnInfo info = ColumnMapper.toColumnInfo(po);
        assertNotNull(info);
        assertTrue(info.getExtractValueEnabled());
    }

    @Test
    @DisplayName("extractValueEnabled field conversion - Model to PO")
    void testColumnInfoPOExtractValueFields() {
        ColumnInfo info = new ColumnInfo();
        info.setExtractValueEnabled(true);

        ColumnInfoPO po = ColumnMapper.toColumnInfoPO(info);
        assertNotNull(po);
        assertTrue(po.isExtractValueEnabled());
    }

    @Test
    @DisplayName("default value conversion - PO to Model")
    void testDefaultValuesToModel() {
        ColumnInfoPO po = new ColumnInfoPO();

        ColumnInfo info = ColumnMapper.toColumnInfo(po);
        assertNotNull(info);
        assertNotNull(info.getExtractValueEnabled());
        assertEquals(false, info.getExtractValueEnabled());
    }

    @Test
    @DisplayName("default value conversion - Model to PO")
    void testDefaultValuesToPO() {
        ColumnInfo info = new ColumnInfo();

        ColumnInfoPO po = ColumnMapper.toColumnInfoPO(info);
        assertNotNull(po);
        assertFalse(po.isExtractValueEnabled());
        assertNotNull(po.getAliases());
        assertTrue(po.getAliases().isEmpty());
    }

    @Test
    @DisplayName("null handling - PO to Model")
    void testNullAliasesPOToModel() {
        ColumnInfoPO po = new ColumnInfoPO();
        po.setAliases(null);

        ColumnInfo info = ColumnMapper.toColumnInfo(po);
        assertNotNull(info);
        assertNotNull(info.getAliases());
        assertTrue(info.getAliases().isEmpty());
    }
}
