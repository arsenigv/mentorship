package org.nakrut.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nakrut.controller.dto.CreateUserRequest;
import org.nakrut.controller.dto.UpdateUserRequest;
import org.nakrut.controller.dto.UserResponse;
import org.nakrut.exception.ResourceConflictException;
import org.nakrut.exception.ResourceNotFoundException;
import org.nakrut.model.User;
import org.nakrut.repository.TaskRepository;
import org.nakrut.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(findUser(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ResourceConflictException("Username already exists: " + username);
        }

        return toResponse(userRepository.save(new User(username)));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findUser(id);
        String username = request.username().trim();
        if (userRepository.existsByUsernameAndIdNot(username, id)) {
            throw new ResourceConflictException("Username already exists: " + username);
        }

        user.setUsername(username);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = findUser(id);
        if (taskRepository.existsByUserId(id)) {
            throw new ResourceConflictException("Cannot delete user with assigned tasks: " + id);
        }

        userRepository.delete(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername());
    }
}
