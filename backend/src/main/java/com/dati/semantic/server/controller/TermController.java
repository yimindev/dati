package com.dati.semantic.server.controller;

import com.dati.semantic.domain.model.Term;
import com.dati.semantic.domain.service.TermService;
import com.dati.semantic.server.assembler.TermAssembler;
import com.dati.semantic.server.pojo.request.CreateTermRequest;
import com.dati.semantic.server.pojo.request.LinkTermRelationRequest;
import com.dati.semantic.server.pojo.request.UpdateTermRequest;
import com.dati.semantic.server.pojo.vo.TermVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;
    private final TermAssembler termAssembler;

    @PostMapping("/subjects/{subjectId}/terms")
    @ResponseStatus(HttpStatus.CREATED)
    public TermVO createTerm(@PathVariable String subjectId, @RequestBody CreateTermRequest request) {
        Term term = termService.createTerm(subjectId, request.getName(), request.getDescription(), request.getAliases());
        return termAssembler.toVO(term);
    }

    @PutMapping("/terms/{id}")
    public TermVO updateTerm(@PathVariable String id, @RequestBody UpdateTermRequest request) {
        Term term = termService.updateTerm(id, request.getName(), request.getDescription(), request.getAliases());
        return termAssembler.toVO(term);
    }

    @DeleteMapping("/terms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTerm(@PathVariable String id) {
        termService.deleteTerm(id);
    }

    @GetMapping("/terms/{id}")
    public TermVO getTerm(@PathVariable String id) {
        Term term = termService.getTermByIdWithRelations(id);
        return termAssembler.toVO(term, term.getRelations());
    }

    @GetMapping("/subjects/{subjectId}/terms")
    public List<TermVO> getTermsBySubject(@PathVariable String subjectId) {
        return termService.getTermsBySubject(subjectId).stream()
                .map(termAssembler::toVO)
                .toList();
    }

    @PostMapping("/terms/{id}/relations")
    @ResponseStatus(HttpStatus.CREATED)
    public void linkTermRelation(@PathVariable String id, @RequestBody LinkTermRelationRequest request) {
        String fieldName = "_".equals(request.getFieldName()) ? null : request.getFieldName();
        termService.linkEntity(id, request.getEntityType(), request.getTableId(), fieldName);
    }

    @DeleteMapping("/terms/{id}/relations/{tableId}/{fieldName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkTermRelation(@PathVariable String id, @PathVariable String tableId, @PathVariable String fieldName) {
        String actualFieldName = "_".equals(fieldName) ? null : fieldName;
        termService.unlinkEntity(id, tableId, actualFieldName);
    }
}
