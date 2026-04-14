package com.dati.semantic.server.pojo.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateSubjectRequest {
    private String name;
    private String description;
    private List<String> aliases;
}
