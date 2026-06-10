package com.dati.semantic.server.controller;

import com.dati.base.pojo.IdResponse;
import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import com.dati.semantic.domain.model.Term;
import com.dati.semantic.domain.service.TermService;
import com.dati.semantic.server.assembler.TermAssembler;
import com.dati.semantic.server.pojo.request.CreateTermRequest;
import com.dati.semantic.server.pojo.request.LinkTermRelationRequest;
import com.dati.semantic.server.pojo.request.UpdateTermRequest;
import com.dati.semantic.server.pojo.vo.TermVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
        Term term = termService.createTerm(subjectId, request.getName(), request.getDescription(), request.getAliases());
        return new IdResponse(term.getId());
    }

    @PutMapping("/terms/{id}")
    public IdResponse updateTerm(@PathVariable String id, @Valid @RequestBody UpdateTermRequest request) {
        termService.updateTerm(id, request.getName(), request.getDescription(), request.getAliases());
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
            PageReq pageReq,
            @RequestParam(required = false) String keyword) {
        Sort sortBy = Sort.by(Sort.Direction.DESC, "updatedAt");
        Page<TermVO> termVOPage = termService
                .getTermsBySubject(subjectId, keyword, pageReq.toPageRequest().withSort(sortBy))
                .map(termAssembler::toVO);
        return PageResponse.of(termVOPage);
    }

    @PostMapping("/terms/{id}/relations")
    public IdResponse linkTermRelation(@PathVariable String id, @Valid @RequestBody LinkTermRelationRequest request) {
        String fieldName = "_".equals(request.getFieldName()) ? null : request.getFieldName();
        termService.linkEntity(id, request.getEntityType(), request.getTableId(), fieldName);
        return new IdResponse(id);
    }

    @DeleteMapping("/terms/{id}/relations/{tableId}/{fieldName}")
    public IdResponse unlinkTermRelation(@PathVariable String id, @PathVariable String tableId, @PathVariable String fieldName) {
        String actualFieldName = "_".equals(fieldName) ? null : fieldName;
        termService.unlinkEntity(id, tableId, actualFieldName);
        return new IdResponse(id);
    }
}
