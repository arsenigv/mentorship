package org.nakrut.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.nakrut.model.Category;
import org.nakrut.model.TaskStatus;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;

class CacheSerializationTests {

    private final JdkSerializationRedisSerializer serializer = new JdkSerializationRedisSerializer();

    @Test
    void serializesCachedUserResponses() {
        UserResponse response = new UserResponse(1L, "arseni");

        assertThat(roundTrip(response)).isEqualTo(response);
        assertThat(roundTrip(List.of(response))).isEqualTo(List.of(response));
    }

    @Test
    void serializesCachedTaskResponses() {
        TaskResponse response = new TaskResponse(
                1L,
                "Learn Spring",
                "Build a CRUD API",
                TaskStatus.TODO,
                Category.EDUCATION,
                1L
        );

        assertThat(roundTrip(response)).isEqualTo(response);
        assertThat(roundTrip(List.of(response))).isEqualTo(List.of(response));
    }

    private Object roundTrip(Object value) {
        return serializer.deserialize(serializer.serialize(value));
    }
}
