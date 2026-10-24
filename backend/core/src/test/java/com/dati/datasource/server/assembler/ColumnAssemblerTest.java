package com.dati.datasource.server.assembler;

import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.server.pojo.ColumnInfoVO;
import com.dati.datasource.server.pojo.request.UpdateColumnRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColumnAssemblerTest {

    private final ColumnAssembler columnAssembler = new ColumnAssembler();

    @Test
    @DisplayName("extractValueEnabled field conversion - Model to VO")
    void testAssemblerWithExtractValueFields() {
        ColumnInfo info = new ColumnInfo();
        info.setExtractValueEnabled(true);

        ColumnInfoVO vo = columnAssembler.toColumnInfoVO(info);
        assertTrue(vo.getExtractValueEnabled());
    }

    @Test
    @DisplayName("extractValueEnabled field conversion - request to Model")
    void testAssemblerWithExtractValueFieldsReverse() {
        UpdateColumnRequest vo = new UpdateColumnRequest();
        vo.setExtractValueEnabled(true);

        ColumnInfo info = columnAssembler.toColumnInfo(vo);
        assertTrue(info.getExtractValueEnabled());
    }
}
