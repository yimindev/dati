package com.dati.semantic.server.controller;

import com.dati.base.pojo.IdResponse;
import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import com.dati.semantic.domain.model.Term;
import com.dati.semantic.domain.model.TermRelation;
import com.dati.semantic.domain.service.TermService;
import com.dati.semantic.server.assembler.TermAssembler;
import com.dati.semantic.server.pojo.request.CreateTermRequest;
import com.dati.semantic.server.pojo.request.LinkTermRelationRequest;
import com.dati.semantic.server.pojo.request.UpdateTermRequest;
import com.dati.semantic.server.pojo.vo.TermVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;
    private final TermAssembler termAssembler;

    @PostMapping("/subjects/{subjectId}/terms")
    public IdResponse createTerm(@PathVariable String subjectId, @Valid @RequestBody CreateTermRequest request) {
        Term term = new Term();
        term.setSubjectId(subjectId);
        term.setName(request.getName());
        term.setDescription(request.getDescription());
        term.setAliases(request.getAliases());
        termAssembler.fillUsersFromRequest(term);
        return new IdResponse(termService.createTerm(term).getId());
    }

    @PutMapping("/terms/{id}")
    public IdResponse updateTerm(@PathVariable String id, @Valid @RequestBody UpdateTermRequest request) {
        Term term = new Term();
        term.setName(request.getName());
        term.setDescription(request.getDescription());
        term.setAliases(request.getAliases());
        termAssembler.fillUpdateUserFromRequest(term);
        termService.updateTerm(id, term);
        return new IdResponse(id);
    }

    @DeleteMapping("/terms/{id}")
    public IdResponse deleteTerm(@PathVariable String id) {
        termService.deleteTerm(id);
        return new IdResponse(id);
    }

    @GetMapping("/terms/{id}")
    public TermVO getTerm(@PathVariable String id) {
        Term term = termService.getTermByIdWithRelations(id);
        return termAssembler.toVO(term, term.getRelations());
    }

    @GetMapping("/subjects/{subjectId}/terms")
    public PageResponse<TermVO> getTermsBySubject(
            @PathVariable String subjectId,
            @Valid PageReq pageReq,
            @RequestParam(required = false) String keyword) {
        Sort sortBy = Sort.by(Sort.Direction.DESC, "updatedAt");
        return termAssembler.toPageResponse(
                termService.getTermsBySubject(subjectId, keyword, pageReq.toPageRequest().withSort(sortBy)));
    }

    @PostMapping("/terms/{id}/relations")
    public IdResponse linkTermRelation(@PathVariable String id, @Valid @RequestBody LinkTermRelationRequest request) {
        TermRelation relation = termService.linkEntity(
                id, request.getEntityType(), request.getTableId(), request.getFieldName());
        return new IdResponse(relation.getId());
    }

    @DeleteMapping("/terms/{id}/relations/{relationId}")
    public IdResponse unlinkRelationById(@PathVariable String id, @PathVariable String relationId) {
        termService.unlinkRelation(id, relationId);
        return new IdResponse(relationId);
    }
}
