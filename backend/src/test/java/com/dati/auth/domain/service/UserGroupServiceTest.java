package com.dati.auth.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserGroupService unit tests")
class UserGroupServiceTest {

    private final UserGroupService service = new UserGroupService();

    @Test
    void everyUserImplicitlyBelongsToAllUsersGroup() {
        assertThat(service.groupIdsOf("any-user-id"))
                .containsExactly(UserGroupService.ALL_USERS);
    }
}
