package org.nakrut.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

public record UserResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "arseni") String username
) implements Serializable {
}
