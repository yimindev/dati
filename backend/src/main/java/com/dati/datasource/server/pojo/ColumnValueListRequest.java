package com.dati.datasource.server.pojo;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ColumnValueListRequest {
    private List<ValueItemVO> values = new ArrayList<>();
    private List<String> deletedIds = new ArrayList<>();

    @Data
    public static class ValueItemVO {
        private String id;
        private String value;
        private List<String> synonyms = new ArrayList<>();
    }
}
