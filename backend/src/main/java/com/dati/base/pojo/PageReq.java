package com.dati.base.pojo;

import lombok.Data;
import org.springframework.data.domain.PageRequest;

@Data
public class PageReq {

    private int page = 1;

    private int size = 10;

    // 可以添加其他通用分页相关的方法
    public int getOffset() {
        return (page - 1) * size;
    }

    public int getLimit() {
        return size;
    }

    public PageRequest toPageRequest() {
        return PageRequest.of(page - 1, size);
    }

}
