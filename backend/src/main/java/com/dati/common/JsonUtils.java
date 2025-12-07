package com.dati.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class JsonUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Convert object to JSON failed", e);
            throw new RuntimeException("Convert to JSON failed", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Parse JSON to object failed", e);
            throw new RuntimeException("Parse JSON failed", e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("Parse JSON to object failed", e);
            throw new RuntimeException("Parse JSON failed", e);
        }
    }

    public static JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Parse JSON string failed", e);
            throw new RuntimeException("Parse JSON failed", e);
        }
    }

    public static Map<String, Object> toMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Convert JSON to Map failed", e);
            throw new RuntimeException("Convert to Map failed", e);
        }
    }

    public static <T> List<T> toList(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            log.error("Convert JSON to List failed", e);
            throw new RuntimeException("Convert to List failed", e);
        }
    }

    public static boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    public static ObjectNode createObjectNode() {
        return objectMapper.createObjectNode();
    }
}
