package com.dati.base.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@ContextConfiguration(classes = {GlobalExceptionHandlerTest.TestController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return 404 for DatiException with NOT_FOUND")
    void datiException_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    @DisplayName("Should return 400 for DatiException with BAD_REQUEST")
    void datiException_badRequest_shouldReturn400() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    @DisplayName("Should return 400 for DatiException with parameterized message")
    void datiException_parameterized_shouldReturn400WithResolvedMessage() throws Exception {
        mockMvc.perform(get("/test/sql-error"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("DS003"))
            .andExpect(jsonPath("$.message").value("SQL execution error: Table users not found"));
    }

    @Test
    @DisplayName("Should return 500 for legacy DatiException")
    void datiException_legacy_shouldReturn500() throws Exception {
        mockMvc.perform(get("/test/legacy"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.getCode()))
            .andExpect(jsonPath("$.message").value("Legacy error message"));
    }

    @Test
    @DisplayName("Should return 500 for unexpected exceptions")
    void unexpectedException_shouldReturn500() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.getCode()))
            .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/not-found")
        public String notFound() {
            throw new DatiException(ErrorCode.NOT_FOUND);
        }

        @GetMapping("/bad-request")
        public String badRequest() {
            throw new DatiException(ErrorCode.INVALID_PARAMETER);
        }

        @GetMapping("/sql-error")
        public String sqlError() {
            throw new DatiException(ErrorCode.DS_SQL_ERROR, "Table users not found");
        }

        @GetMapping("/legacy")
        public String legacy() {
            throw new DatiException("Legacy error message");
        }

        @GetMapping("/unexpected")
        public String unexpected() {
            throw new RuntimeException("Something unexpected");
        }
    }
}
