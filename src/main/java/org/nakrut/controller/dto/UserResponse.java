package org.nakrut.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "arseni") String username
) {
}
