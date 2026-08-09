package com.dati.auth.authentication;

import com.dati.semantic.repository.dao.SemanticSearchDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Authentication provider chain ordering tests")
class ProviderOrderingTest {

    @MockitoBean
    private SemanticSearchDAO semanticSearchDAO;

    @Autowired
    private List<AuthenticationProvider> providers;

    @Test
    void apiKeyProviderComesBeforeJwtProviderInChain() {
        int apiKeyIndex = indexOf(ApiKeyAuthenticationProvider.class);
        int localIndex = indexOf(LocalAuthenticationProvider.class);

        assertThat(apiKeyIndex).isNotNegative();
        assertThat(localIndex).isNotNegative();
        assertThat(apiKeyIndex).isLessThan(localIndex);
    }

    private int indexOf(Class<? extends AuthenticationProvider> type) {
        for (int i = 0; i < providers.size(); i++) {
            if (type.isInstance(providers.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
