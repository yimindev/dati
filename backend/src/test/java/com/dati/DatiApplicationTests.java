package com.dati;

import com.dati.semantic.repository.dao.SemanticSearchDAO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class DatiApplicationTests {

    @MockitoBean
    private SemanticSearchDAO semanticSearchDAO;

    @Test
    void contextLoads() {
    }

}
