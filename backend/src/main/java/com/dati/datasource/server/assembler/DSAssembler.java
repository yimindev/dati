package com.dati.datasource.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.base.pojo.PageResponse;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.server.pojo.DatasourceVO;
import com.dati.db.JdbcConnector;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DSAssembler extends BaseAssembler {

    public static JdbcConnector toJdbcConnector(DataSource dataSource) {
        return new JdbcConnector(dataSource.getJdbcUrl(), dataSource.getUsername(), dataSource.getPassword());
    }

    public DatasourceVO toDatasourceVO(DataSource dataSource) {
        DatasourceVO vo = mapFields(dataSource);
        super.fillUserInfo(List.of(vo));
        return vo;
    }

    public PageResponse<DatasourceVO> toPageResponse(Page<DataSource> page) {
        List<DatasourceVO> vos = page.getContent().stream()
                .map(this::mapFields)
                .collect(Collectors.toList());
        super.fillUserInfo(vos);
        return PageResponse.of(new PageImpl<>(vos, page.getPageable(), page.getTotalElements()));
    }

    private DatasourceVO mapFields(DataSource dataSource) {
        DatasourceVO vo = new DatasourceVO();
        super.copyBaseInfo(dataSource, vo);
        vo.setJdbcUrl(dataSource.getJdbcUrl());
        vo.setUsername(dataSource.getUsername());
        vo.setType(dataSource.getType());
        vo.setDefaultSchema(dataSource.getDefaultSchema());
        return vo;
    }

}
