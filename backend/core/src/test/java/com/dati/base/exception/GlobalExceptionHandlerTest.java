package com.dati.base.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("Should return 404 for unmapped routes (NoResourceFoundException)")
    void unmappedRoute_shouldReturn404() throws Exception {
        mockMvc.perform(get("/test/unknown-route-xyz"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CM002"));
    }

    @Test
    @DisplayName("Should return 400 for missing required request parameter")
    void missingRequiredParam_shouldReturn400() throws Exception {
        mockMvc.perform(get("/test/required-param"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETER.getCode()))
            .andExpect(jsonPath("$.message").value(containsString("Required request parameter 'schema'")));
    }

    @Test
    @DisplayName("Should return 400 for request body element validation failure")
    void bodyElementValidationFailure_shouldReturn400() throws Exception {
        mockMvc.perform(post("/test/validate-elements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"name\":\"artist\"}]"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETER.getCode()));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/not-found")
        public String notFound() {
            throw new DatiException(ErrorCode.NOT_FOUND);
        }

        @GetMapping("/required-param")
        public String requiredParam(@RequestParam String schema) {
            return "ok";
        }

        @PostMapping("/validate-elements")
        public String validateElements(@Valid @RequestBody List<@Valid ValidateItem> items) {
            return "ok";
        }

        @Setter
        @Getter
        static class ValidateItem {

            @NotBlank
            private String schema;

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
