package com.dataconnai.base.pojo;

import lombok.Data;

@Data
public class IdResponse {
    private String id;

    public IdResponse(String id) {
        this.id = id;
    }
}
