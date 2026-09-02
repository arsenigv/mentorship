package org.nakrut.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Mentorship API",
                version = "v1",
                description = "REST API for managing users and their assigned tasks.",
                license = @License(name = "Unlicensed")
        ),
        tags = {
                @Tag(name = "Tasks", description = "Create, read, update, delete, page, and sort tasks"),
                @Tag(name = "Users", description = "Create, read, update, and delete task owners")
        }
)
public class OpenApiConfiguration {
}
