package com.dati.semantic.server.controller;

import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.domain.service.SubjectService;
import com.dati.semantic.server.assembler.SubjectAssembler;
import com.dati.semantic.server.pojo.request.AddTableToSubjectRequest;
import com.dati.semantic.server.pojo.request.CreateSubjectRequest;
import com.dati.semantic.server.pojo.request.UpdateSubjectRequest;
import com.dati.semantic.server.pojo.vo.SubjectDetailVO;
import com.dati.semantic.server.pojo.vo.SubjectTableVO;
import com.dati.semantic.server.pojo.vo.SubjectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;
    private final SubjectAssembler subjectAssembler;

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
    public SubjectDetailVO getSubject(@PathVariable String id) {
        return subjectAssembler.toDetailVO(subjectService.getSubjectWithTables(id));
    }

    @GetMapping("/{id}/tables")
    public List<SubjectTableVO> getSubjectTables(@PathVariable String id) {
        return subjectService.getSubjectWithTables(id).getTables().stream()
                .map(subjectAssembler::toSubjectTableVO)
                .toList();
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
    public List<SubjectVO> getSubjects(@RequestParam(required = false) String datasourceId) {
        return subjectService.getSubjectsByDatasource(datasourceId).stream()
                .map(subjectAssembler::toVO)
                .toList();
    }
}
