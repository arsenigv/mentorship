package org.nakrut.mapper;

import org.nakrut.dto.CreateUserRequest;
import org.nakrut.dto.UpdateUserRequest;
import org.nakrut.dto.UserResponse;
import org.nakrut.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(CreateUserRequest request) {
        return new User(normalizedUsername(request));
    }

    public void updateEntity(UpdateUserRequest request, User user) {
        user.setUsername(normalizedUsername(request));
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername());
    }

    public String normalizedUsername(CreateUserRequest request) {
        return request.username().trim();
    }

    public String normalizedUsername(UpdateUserRequest request) {
        return request.username().trim();
    }
}
