package org.nakrut.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.nakrut.dto.CreateUserRequest;
import org.nakrut.dto.UpdateUserRequest;
import org.nakrut.dto.UserResponse;
import org.nakrut.model.User;
import org.springframework.test.util.ReflectionTestUtils;

class UserMapperTests {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void mapsCreateRequestToEntityWithNormalizedUsername() {
        User user = userMapper.toEntity(new CreateUserRequest("  arseni  "));

        assertThat(user.getUsername()).isEqualTo("arseni");
    }

    @Test
    void mapsUpdateRequestToExistingEntity() {
        User user = new User("old-name");

        userMapper.updateEntity(new UpdateUserRequest("  new-name  "), user);

        assertThat(user.getUsername()).isEqualTo("new-name");
    }

    @Test
    void mapsEntityToResponse() {
        User user = new User("arseni");
        ReflectionTestUtils.setField(user, "id", 1L);

        UserResponse response = userMapper.toResponse(user);

        assertThat(response).isEqualTo(new UserResponse(1L, "arseni"));
    }
}
