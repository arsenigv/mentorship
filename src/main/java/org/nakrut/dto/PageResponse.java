package org.nakrut.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "One page of results and its pagination metadata")
public record PageResponse<T>(
        @Schema(description = "Items in the requested page")
        List<T> content,
        @Schema(description = "Zero-based page number", example = "0")
        int page,
        @Schema(description = "Number of items requested per page", example = "20")
        int size,
        @Schema(description = "Total number of matching items", example = "42")
        long totalElements,
        @Schema(description = "Total number of pages", example = "3")
        int totalPages,
        @Schema(description = "Whether this is the first page", example = "true")
        boolean first,
        @Schema(description = "Whether this is the last page", example = "false")
        boolean last
) implements Serializable {

    public static <T> PageResponse<T> from(Page<T> source) {
        return new PageResponse<>(
                List.copyOf(source.getContent()),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast()
        );
    }
}
