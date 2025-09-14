package com.dataconnai.base.pojo;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {
    private List<T> data;
    private int total;
    private int page = 1;
    private int size = 10;

    public int getTotalPages() {
        return size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }
}
