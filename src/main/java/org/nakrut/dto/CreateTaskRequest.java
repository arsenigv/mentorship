package org.nakrut.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.nakrut.model.Category;

public record CreateTaskRequest(
        @Schema(example = "Learn Spring Boot") @NotBlank @Size(max = 255) String title,
        @Schema(example = "Build a CRUD REST API") String description,
        @Schema(example = "2026-09-10") @NotNull LocalDate dueDate,
        @Schema(example = "EDUCATION") @NotNull Category category,
        @Schema(example = "1") @NotNull @Positive Long userId
) {
}
