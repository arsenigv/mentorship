package org.nakrut.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.nakrut.model.Category;

public record CreateTaskRequest(
        @Schema(example = "Learn Spring Boot") @NotBlank String title,
        @Schema(example = "Build a CRUD REST API") String description,
        @Schema(example = "EDUCATION") @NotNull Category category,
        @Schema(example = "1") @NotNull Long userId
) {
}
