package com.dati.datasource.server.assembler;

import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.server.pojo.ColumnInfoVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColumnAssemblerTest {

    private final ColumnAssembler columnAssembler = new ColumnAssembler();

    @Test
    @DisplayName("测试 extractValueEnabled 字段转换 - Model 到 VO")
    void testAssemblerWithExtractValueFields() {
        ColumnInfo info = new ColumnInfo();
        info.setExtractValueEnabled(true);

        ColumnInfoVO vo = columnAssembler.toColumnInfoVO(info);
        assertTrue(vo.getExtractValueEnabled());
    }

    @Test
    @DisplayName("测试 extractValueEnabled 字段转换 - VO 到 Model")
    void testAssemblerWithExtractValueFieldsReverse() {
        ColumnInfoVO vo = new ColumnInfoVO();
        vo.setExtractValueEnabled(true);

        ColumnInfo info = columnAssembler.toColumnInfo(vo);
        assertTrue(info.getExtractValueEnabled());
    }
}
