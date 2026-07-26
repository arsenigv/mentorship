package org.nakrut.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nakrut.model.Category;

class CreateTaskRequestTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsTitleLongerThan255Characters() {
        CreateTaskRequest request = new CreateTaskRequest(
                "a".repeat(256),
                "Build a CRUD API",
                Category.EDUCATION,
                1L
        );

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("title"));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveUserId(long userId) {
        CreateTaskRequest request = new CreateTaskRequest(
                "Learn Spring Boot",
                "Build a CRUD API",
                Category.EDUCATION,
                userId
        );

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("userId"));
    }

    @Test
    void acceptsValidRequest() {
        CreateTaskRequest request = new CreateTaskRequest(
                "Learn Spring Boot",
                "Build a CRUD API",
                Category.EDUCATION,
                1L
        );

        assertThat(validator.validate(request)).isEmpty();
    }
}
