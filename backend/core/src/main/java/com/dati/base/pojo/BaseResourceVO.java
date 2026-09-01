package com.dati.base.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BaseResourceVO extends BaseResource {

    private String createdUserName;

    private String updatedUserName;

}
