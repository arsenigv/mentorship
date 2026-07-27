package org.nakrut.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "users")
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "usersById", key = "#id")
    public UserResponse findById(Long id) {
        return userMapper.toResponse(findUser(id));
    }

    @Transactional
    @CacheEvict(cacheNames = "users", allEntries = true)
    public UserResponse create(CreateUserRequest request) {
        String username = userMapper.normalizedUsername(request);
        if (userRepository.existsByUsername(username)) {
            log.warn("User creation rejected: username already exists");
            throw new DuplicateUsernameException(username);
        }

        User savedUser = userRepository.save(userMapper.toEntity(request));
        log.info("User created: id={}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "users", allEntries = true),
            @CacheEvict(cacheNames = "usersById", key = "#id")
    })
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findUser(id);
        String username = userMapper.normalizedUsername(request);
        if (userRepository.existsByUsernameAndIdNot(username, id)) {
            log.warn("User update rejected: id={}, username already exists", id);
            throw new DuplicateUsernameException(username);
        }

        userMapper.updateEntity(request, user);
        User savedUser = userRepository.save(user);
        log.info("User updated: id={}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "users", allEntries = true),
            @CacheEvict(cacheNames = "usersById", key = "#id")
    })
    public void delete(Long id) {
        User user = findUser(id);
        if (taskRepository.existsByUserId(id)) {
            log.warn("User deletion rejected: id={}, assigned tasks exist", id);
            throw new UserHasAssignedTasksException(id);
        }

        userRepository.delete(user);
        log.info("User deleted: id={}", id);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found: id={}", id);
                    return new ResourceNotFoundException("User not found: " + id);
                });
    }
}
