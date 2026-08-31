package org.nakrut.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@ConditionalOnProperty(
        prefix = "spring.cache",
        name = "type",
        havingValue = "redis"
)
public class CacheConfiguration {
}
