package org.nakrut.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.nakrut.model.Category;
import org.nakrut.model.TaskStatus;

class UpdateTaskRequestTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsMissingStatus() {
        UpdateTaskRequest request = new UpdateTaskRequest(
                "Learn Spring Boot",
                "Build a CRUD API",
                null,
                Category.EDUCATION
        );

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("status"));
    }

    @Test
    void rejectsTitleLongerThan255Characters() {
        UpdateTaskRequest request = new UpdateTaskRequest(
                "a".repeat(256),
                "Build a CRUD API",
                TaskStatus.IN_PROGRESS,
                Category.EDUCATION
        );

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("title"));
    }

    @Test
    void acceptsValidRequest() {
        UpdateTaskRequest request = new UpdateTaskRequest(
                "Learn Spring Boot",
                "Build a CRUD API",
                TaskStatus.IN_PROGRESS,
                Category.EDUCATION
        );

        assertThat(validator.validate(request)).isEmpty();
    }
}
