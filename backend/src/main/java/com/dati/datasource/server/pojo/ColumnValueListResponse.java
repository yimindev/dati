package com.dati.datasource.server.pojo;

import com.dati.datasource.domain.service.ColumnValueService;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ColumnValueListResponse {
    private List<ValueItemVO> values;

    public ColumnValueListResponse() {}

    public ColumnValueListResponse(List<ColumnValueService.ValueItem> items) {
        this.values = items == null ? new ArrayList<>() : items.stream().map(item -> {
            ValueItemVO vo = new ValueItemVO();
            vo.setId(item.getId());
            vo.setValue(item.getValue());
            vo.setSynonyms(item.getSynonyms());
            return vo;
        }).toList();
    }

    @Data
    public static class ValueItemVO {
        private String id;
        private String value;
        private List<String> synonyms;
    }
}
