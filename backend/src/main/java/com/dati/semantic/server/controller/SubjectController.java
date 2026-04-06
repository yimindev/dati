package com.dati.semantic.server.controller;

import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.domain.service.SubjectService;
import com.dati.semantic.server.assembler.SubjectAssembler;
import com.dati.semantic.server.pojo.request.AddTableToSubjectRequest;
import com.dati.semantic.server.pojo.request.CreateSubjectRequest;
import com.dati.semantic.server.pojo.request.UpdateSubjectRequest;
import com.dati.semantic.server.pojo.vo.SubjectAvailableTableVO;
import com.dati.semantic.server.pojo.vo.SubjectVO;
import com.dati.datasource.server.assembler.TableAssembler;
import com.dati.datasource.server.pojo.TableInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;
    private final SubjectAssembler subjectAssembler;
    private final TableAssembler tableAssembler;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectVO createSubject(@RequestBody CreateSubjectRequest request) {
        Subject subject = subjectService.createSubject(
                request.getName(),
                request.getDescription(),
                request.getDatasourceId()
        );
        return subjectAssembler.toVO(subject);
    }

    @PutMapping("/{id}")
    public SubjectVO updateSubject(@PathVariable String id, @RequestBody UpdateSubjectRequest request) {
        Subject subject = subjectService.updateSubject(id, request.getName(), request.getDescription());
        return subjectAssembler.toVO(subject);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(@PathVariable String id) {
        subjectService.deleteSubject(id);
    }

    @GetMapping("/{id}")
    public SubjectVO getSubject(@PathVariable String id) {
        return subjectAssembler.toVO(subjectService.getSubjectById(id));
    }

    @GetMapping("/{id}/tables")
    public List<TableInfoVO> getSubjectTables(@PathVariable String id) {
        return subjectService.getTablesBySubjectId(id).stream()
                .map(tableAssembler::toTableInfoVO)
                .toList();
    }

    @GetMapping("/{id}/available-tables")
    public List<SubjectAvailableTableVO> getAvailableTables(
            @PathVariable String id,
            @RequestParam String schema) {
        return subjectService.getAvailableTables(id, schema);
    }

    @PostMapping("/{id}/tables")
    @ResponseStatus(HttpStatus.CREATED)
    public void addTableToSubject(@PathVariable String id, @RequestBody AddTableToSubjectRequest request) {
        subjectService.addTableToSubject(id, request.getTableId());
    }

    @DeleteMapping("/{id}/tables/{tableId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTableFromSubject(@PathVariable String id, @PathVariable String tableId) {
        subjectService.removeTableFromSubject(id, tableId);
    }

    @GetMapping
    public PageResponse<SubjectVO> getSubjects(PageReq pageReq, @RequestParam(required = false) String datasourceId) {
        Sort sortBy = Sort.by(Sort.Direction.DESC, "updatedAt");
        Page<SubjectVO> subjectVOPage = subjectService.getSubjectsByDatasource(datasourceId, pageReq.toPageRequest().withSort(sortBy))
                .map(subjectAssembler::toVO);
        return PageResponse.of(subjectVOPage);
    }
}
