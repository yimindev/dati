package com.dati.base;

import com.dati.auth.authentication.User;
import com.dati.auth.authentication.UserService;
import com.dati.base.pojo.BaseResource;
import com.dati.base.pojo.BaseResourceVO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BaseAssembler {
    
    private UserService userService;
    
    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }
    
    public void copyBaseInfo(BaseResource source, BaseResource target) {
        target.setId(source.getId());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setCreatedBy(source.getCreatedBy());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedBy(source.getUpdatedBy());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    public void fillUsersFromRequest(BaseResource resource) {
        User user = RequestContext.getUser();
        resource.setCreatedBy(user.getId());
        resource.setUpdatedBy(user.getId());
    }

    public void fillUpdateUserFromRequest(BaseResource resource) {
        User user = RequestContext.getUser();
        resource.setUpdatedBy(user.getId());
    }
    
    public void fillUserInfo(List<? extends BaseResourceVO> baseResourceVOS) {
        Set<String> userIds = baseResourceVOS.stream()
                .flatMap(vo -> Stream.of(vo.getCreatedBy(), vo.getUpdatedBy()))
                .collect(Collectors.toSet());
        Map<String, User> userMap = userService.getUserMap(userIds);
        baseResourceVOS.forEach(vo -> {
            if (vo.getCreatedBy() != null) {
                User user = userMap.get(vo.getCreatedBy());
                if (user != null) {
                    vo.setCreatedUserName(user.getDisplayName());
                }
            }
            if (vo.getUpdatedBy() != null) {
                User user = userMap.get(vo.getUpdatedBy());
                if (user != null) {
                    vo.setUpdatedUserName(user.getDisplayName());
                }
            }
        });
    }
    
}
