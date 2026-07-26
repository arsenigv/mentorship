package org.nakrut.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.nakrut.model.Category;
import org.nakrut.model.TaskStatus;

public record UpdateTaskRequest(
        @Schema(example = "Learn Spring Boot") @NotBlank String title,
        @Schema(example = "Complete the CRUD REST API") String description,
        @Schema(example = "IN_PROGRESS") @NotNull TaskStatus status,
        @Schema(example = "EDUCATION") @NotNull Category category
) {
}
