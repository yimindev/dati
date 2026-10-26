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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@ContextConfiguration(classes = {GlobalExceptionHandlerTest.TestController.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
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
    @DisplayName("Should localize DatiException messages from the request locale")
    void datiException_shouldUseRequestLocale() throws Exception {
        mockMvc.perform(get("/test/sql-error").locale(Locale.SIMPLIFIED_CHINESE))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("DS003"))
            .andExpect(jsonPath("$.message").value("SQL 执行错误：Table users not found"));

        mockMvc.perform(get("/test/sql-error").locale(Locale.ENGLISH))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("SQL execution error: Table users not found"));
    }

    @Test
    @DisplayName("Should localize messages from the Accept-Language header")
    void datiException_shouldUseAcceptLanguageHeader() throws Exception {
        mockMvc.perform(get("/test/sql-error").header("Accept-Language", "zh-CN"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("SQL 执行错误：Table users not found"));

        mockMvc.perform(get("/test/sql-error").header("Accept-Language", "en"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("SQL execution error: Table users not found"));
    }

    @Test
    @DisplayName("Should localize validation prefixes from the request locale")
    void validationMessage_shouldUseRequestLocale() throws Exception {
        mockMvc.perform(post("/test/validate")
                .locale(Locale.SIMPLIFIED_CHINESE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETER.getCode()))
            .andExpect(jsonPath("$.message").value(containsString("请求参数错误：")))
            .andExpect(jsonPath("$.message").value(containsString("jdbc_url: ")));
    }

    @Test
    @DisplayName("Should localize internal errors but keep custom DatiException messages untouched")
    void internalAndCustomMessages_shouldUseRequestLocale() throws Exception {
        mockMvc.perform(get("/test/unexpected").locale(Locale.SIMPLIFIED_CHINESE))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.getCode()))
            .andExpect(jsonPath("$.message").value("服务器内部错误"));

        mockMvc.perform(get("/test/legacy").locale(Locale.SIMPLIFIED_CHINESE))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Legacy error message"));
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

    @Test
    @DisplayName("Should report the invalid enum value and accepted constants")
    void invalidEnumValue_shouldReportAcceptedValues() throws Exception {
        mockMvc.perform(post("/test/deserialize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"H2\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETER.getCode()))
            .andExpect(jsonPath("$.message").value(containsString("type:")))
            .andExpect(jsonPath("$.message").value(containsString("not one of the values accepted for Enum class")))
            .andExpect(jsonPath("$.message").value(containsString("MYSQL")))
            .andExpect(jsonPath("$.message").value(containsString("POSTGRESQL")));
    }

    @Test
    @DisplayName("Should report malformed JSON instead of the generic read failure")
    void malformedJson_shouldReportParseError() throws Exception {
        mockMvc.perform(post("/test/deserialize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETER.getCode()))
            .andExpect(jsonPath("$.message").value(not("Request parameter error: Failed to read request")))
            .andExpect(jsonPath("$.message").value(not(emptyString())));
    }

    @Test
    @DisplayName("Should report the field whose value has the wrong type")
    void wrongValueType_shouldReportField() throws Exception {
        mockMvc.perform(post("/test/deserialize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":{\"a\":1}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETER.getCode()))
            .andExpect(jsonPath("$.message").value(containsString("name")));
    }

    @Test
    @DisplayName("Should use the JSON field name in validation messages")
    void fieldValidation_shouldUseJsonPropertyName() throws Exception {
        mockMvc.perform(post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("jdbc_url: ")));
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

        @PostMapping("/deserialize")
        public String deserialize(@RequestBody DeserializeRequest request) {
            return "ok";
        }

        @PostMapping("/validate")
        public String validate(@Valid @RequestBody ValidateRequest request) {
            return "ok";
        }

        @Setter
        @Getter
        static class DeserializeRequest {
            private TestEnum type;
            private String name;
        }

        enum TestEnum {
            MYSQL, POSTGRESQL
        }

        @Setter
        @Getter
        static class ValidateRequest {
            @NotBlank
            private String jdbcUrl;
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
