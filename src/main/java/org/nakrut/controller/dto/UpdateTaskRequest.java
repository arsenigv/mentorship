package org.nakrut.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.nakrut.model.Category;

public record UpdateTaskRequest(
        @Schema(example = "Learn Spring Boot") @NotBlank String title,
        @Schema(example = "Complete the CRUD REST API") String description,
        @Schema(example = "true") @NotNull Boolean completed,
        @Schema(example = "EDUCATION") @NotNull Category category
) {
}
