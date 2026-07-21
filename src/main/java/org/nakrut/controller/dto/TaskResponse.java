package org.nakrut.controller.dto;

import org.nakrut.model.Category;

public record TaskResponse(
        Long id,
        String title,
        String description,
        boolean completed,
        Category category,
        Long userId
) {
}
