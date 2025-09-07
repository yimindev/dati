package com.dataconnai.base.pojo;

import lombok.Data;

import java.util.List;

@Data
public class ListResponse<T> {
    private List<T> data;
}
