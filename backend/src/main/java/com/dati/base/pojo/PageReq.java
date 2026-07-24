package com.dati.base.pojo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.data.domain.PageRequest;

@Data
public class PageReq {

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 10;

    public PageRequest toPageRequest() {
        return PageRequest.of(page - 1, size);
    }

}
