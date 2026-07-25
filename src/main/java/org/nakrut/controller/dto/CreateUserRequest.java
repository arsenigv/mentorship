package org.nakrut.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @Schema(example = "arseni") @NotBlank @Size(max = 255) String username
) {
}
