package org.nakrut.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nakrut.model.Category;

public record TaskResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Learn Spring Boot") String title,
        @Schema(example = "Build a CRUD REST API") String description,
        @Schema(example = "false") boolean completed,
        @Schema(example = "EDUCATION") Category category,
        @Schema(example = "1") Long userId
) {
}
