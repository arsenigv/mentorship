package org.nakrut.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nakrut.model.Category;
import org.nakrut.model.TaskStatus;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;

class CacheSerializationTests {

    private static final LocalDate DUE_DATE = LocalDate.now().plusDays(7);

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
                DUE_DATE,
                Category.EDUCATION,
                1L
        );

        assertThat(roundTrip(response)).isEqualTo(response);
        assertThat(roundTrip(List.of(response))).isEqualTo(List.of(response));
    }

    @Test
    void serializesCachedTaskPageResponses() {
        var task = new TaskResponse(
                1L,
                "Learn Spring",
                "Build a CRUD API",
                TaskStatus.TODO,
                DUE_DATE,
                Category.EDUCATION,
                1L
        );
        var page = new PageResponse<>(
                List.of(task),
                0,
                20,
                1,
                1,
                true,
                true
        );

        assertThat(roundTrip(page)).isEqualTo(page);
    }


    private Object roundTrip(Object value) {
        return serializer.deserialize(serializer.serialize(value));
    }
}
