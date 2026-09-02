package org.nakrut.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nakrut.model.Category;

class CreateTaskRequestTests {

    private static final LocalDate DUE_DATE = LocalDate.now().plusDays(7);

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsTitleLongerThan255Characters() {
        CreateTaskRequest request = new CreateTaskRequest(
                "a".repeat(256),
                "Build a CRUD API",
                DUE_DATE,
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
                DUE_DATE,
                Category.EDUCATION,
                userId
        );

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("userId"));
    }

    @Test
    void rejectsMissingDueDate() {
        CreateTaskRequest request = new CreateTaskRequest(
                "Learn Spring Boot",
                "Build a CRUD API",
                null,
                Category.EDUCATION,
                1L
        );

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("dueDate"));
    }

    @Test
    void acceptsValidRequest() {
        CreateTaskRequest request = new CreateTaskRequest(
                "Learn Spring Boot",
                "Build a CRUD API",
                DUE_DATE,
                Category.EDUCATION,
                1L
        );

        assertThat(validator.validate(request)).isEmpty();
    }
}
