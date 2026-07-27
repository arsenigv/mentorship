package org.nakrut.exception;

public record FieldValidationError(String field, String message) {
}
