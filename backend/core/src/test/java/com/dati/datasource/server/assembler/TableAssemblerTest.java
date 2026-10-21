package com.dati.datasource.server.assembler;

import com.dati.auth.authentication.User;
import com.dati.auth.domain.service.UserService;
import com.dati.base.pojo.PageResponse;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.server.pojo.TableInfoVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TableAssembler unit tests")
class TableAssemblerTest {

    @Mock
    private UserService userService;

    @Test
    @DisplayName("toPageResponse fills created_user_name and updated_user_name")
    void toPageResponse_shouldFillUserNames() {
        TableAssembler assembler = new TableAssembler();
        assembler.setUserService(userService);

        TableInfo table = new TableInfo();
        table.setId("table-001");
        table.setName("users");
        table.setCreatedBy("u1");
        table.setUpdatedBy("u2");

        Page<TableInfo> page = new PageImpl<>(List.of(table), PageRequest.of(0, 10), 1);
        when(userService.getUserMap(any()))
                .thenReturn(Map.of("u1", user("u1", "alice"), "u2", user("u2", "bob")));

        PageResponse<TableInfoVO> response = assembler.toPageResponse(page);

        assertThat(response.getData()).hasSize(1);
        TableInfoVO vo = response.getData().getFirst();
        assertThat(vo.getCreatedUserName()).isEqualTo("alice");
        assertThat(vo.getUpdatedUserName()).isEqualTo("bob");
    }

    private static User user(String id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }
}
