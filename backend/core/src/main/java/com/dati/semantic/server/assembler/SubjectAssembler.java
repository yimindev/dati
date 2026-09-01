package com.dati.semantic.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.base.pojo.PageResponse;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.server.pojo.vo.SubjectVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SubjectAssembler extends BaseAssembler {

    private final DataSourceService dataSourceService;

    public SubjectAssembler(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    public SubjectVO toVO(Subject subject) {
        if (subject == null) {
            return null;
        }
        SubjectVO vo = mapFields(subject);
        fillDatasourceInfo(List.of(vo));
        super.fillUserInfo(List.of(vo));
        return vo;
    }

    public PageResponse<SubjectVO> toPageResponse(Page<Subject> page) {
        List<SubjectVO> vos = page.getContent().stream()
                .map(this::mapFields)
                .collect(Collectors.toList());
        fillDatasourceInfo(vos);
        super.fillUserInfo(vos);
        return PageResponse.of(new PageImpl<>(vos, page.getPageable(), page.getTotalElements()));
    }

    private SubjectVO mapFields(Subject subject) {
        SubjectVO vo = new SubjectVO();
        super.copyBaseInfo(subject, vo);
        vo.setDatasourceId(subject.getDatasourceId());
        vo.setAliases(subject.getAliases() != null ? subject.getAliases() : new ArrayList<>());
        return vo;
    }

    private void fillDatasourceInfo(List<SubjectVO> vos) {
        Set<String> ids = vos.stream()
                .map(SubjectVO::getDatasourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        Map<String, String> nameMap = dataSourceService.getDataSourceNameMap(ids);
        vos.forEach(vo -> vo.setDatasourceName(nameMap.get(vo.getDatasourceId())));
    }
}
