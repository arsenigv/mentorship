package org.nakrut.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Schema(example = "arseni.updated") @NotBlank @Size(max = 255) String username
) {
}
