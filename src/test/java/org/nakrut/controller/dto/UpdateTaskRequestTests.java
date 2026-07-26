package org.nakrut.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.nakrut.dto.UpdateTaskRequest;
import org.nakrut.model.Category;

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
}
