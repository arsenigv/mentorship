package org.nakrut.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nakrut.dto.CreateUserRequest;
import org.nakrut.dto.UpdateUserRequest;
import org.nakrut.dto.UserResponse;
import org.nakrut.exception.DuplicateUsernameException;
import org.nakrut.exception.ResourceNotFoundException;
import org.nakrut.exception.UserHasAssignedTasksException;
import org.nakrut.mapper.UserMapper;
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
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(findUser(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String username = userMapper.normalizedUsername(request);
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException(username);
        }

        return userMapper.toResponse(userRepository.save(userMapper.toEntity(request)));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findUser(id);
        String username = userMapper.normalizedUsername(request);
        if (userRepository.existsByUsernameAndIdNot(username, id)) {
            throw new DuplicateUsernameException(username);
        }

        userMapper.updateEntity(request, user);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = findUser(id);
        if (taskRepository.existsByUserId(id)) {
            throw new UserHasAssignedTasksException(id);
        }

        userRepository.delete(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
