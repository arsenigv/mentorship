package org.nakrut.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.nakrut.model.Category;
import org.nakrut.model.TaskStatus;

public record UpdateTaskRequest(
        @Schema(example = "Learn Spring Boot") @NotBlank @Size(max = 255) String title,
        @Schema(example = "Complete the CRUD REST API") String description,
        @Schema(example = "IN_PROGRESS") @NotNull TaskStatus status,
        @Schema(example = "2026-09-10") @NotNull LocalDate dueDate,
        @Schema(example = "EDUCATION") @NotNull Category category
) {
}
