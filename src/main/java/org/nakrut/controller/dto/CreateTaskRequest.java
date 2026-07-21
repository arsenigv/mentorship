package org.nakrut.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.nakrut.model.Category;

public record CreateTaskRequest(
        @NotBlank String title,
        String description,
        @NotNull Category category,
        @NotNull Long userId
) {
}
