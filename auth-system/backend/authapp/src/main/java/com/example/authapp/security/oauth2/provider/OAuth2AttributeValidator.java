package com.example.authapp.security.oauth2.provider;

import java.util.Map;

final class OAuth2AttributeValidator {

    private OAuth2AttributeValidator() {
    }

    static String requiredString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("OAuth2 " + key + " is required");
        }
        return value.toString();
    }

    static String optionalString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> optionalMap(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (!(value instanceof Map<?, ?>)) {
            return null;
        }
        return (Map<String, Object>) value;
    }

    static void requireTrue(Map<String, Object> attributes, String key) {
        if (!Boolean.TRUE.equals(attributes.get(key))) {
            throw new IllegalArgumentException("OAuth2 " + key + " must be true");
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> requiredMap(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("OAuth2 " + key + " is required");
        }
        return (Map<String, Object>) value;
    }
    
}
