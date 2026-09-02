package org.nakrut.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDate;
import org.nakrut.model.Category;
import org.nakrut.model.TaskStatus;

public record TaskResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Learn Spring Boot") String title,
        @Schema(example = "Build a CRUD REST API") String description,
        @Schema(example = "TODO") TaskStatus status,
        @Schema(example = "2026-09-10") LocalDate dueDate,
        @Schema(example = "EDUCATION") Category category,
        @Schema(example = "1") Long userId
) implements Serializable {
}
