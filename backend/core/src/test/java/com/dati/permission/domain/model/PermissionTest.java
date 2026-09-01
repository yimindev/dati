package com.dati.permission.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Permission level comparison tests")
class PermissionTest {

    @Test
    void editCoversView() {
        assertThat(Permission.EDIT.covers(Permission.VIEW)).isTrue();
    }

    @Test
    void editCoversEdit() {
        assertThat(Permission.EDIT.covers(Permission.EDIT)).isTrue();
    }

    @Test
    void viewDoesNotCoverEdit() {
        assertThat(Permission.VIEW.covers(Permission.EDIT)).isFalse();
    }
}
