package org.nakrut.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.nakrut.model.Category;

public record UpdateTaskRequest(
        @NotBlank String title,
        String description,
        @NotNull Boolean completed,
        @NotNull Category category
) {
}
