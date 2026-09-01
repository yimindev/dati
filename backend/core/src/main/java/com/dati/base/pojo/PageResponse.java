package com.dati.base.pojo;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PageResponse<T> {
    private List<T> data;
    private long total;
    private int page = 1;
    private int size = 10;

    public int getTotalPages() {
        return size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        PageResponse<T> pageResponse = new PageResponse<>();
        pageResponse.setData(page.getContent());
        pageResponse.setPage(page.getNumber() + 1);
        pageResponse.setTotal(page.getTotalElements());
        pageResponse.setSize(page.getSize());
        return pageResponse;
    }
}
