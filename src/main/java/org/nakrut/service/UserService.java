package org.nakrut.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nakrut.config.CacheNames;
import org.nakrut.dto.CreateUserRequest;
import org.nakrut.dto.UpdateUserRequest;
import org.nakrut.dto.UserResponse;
import org.nakrut.exception.DuplicateUsernameException;
import org.nakrut.exception.ResourceNotFoundException;
import org.nakrut.exception.UserHasAssignedTasksException;
import org.nakrut.mapper.UserMapper;
import org.nakrut.metrics.ApplicationMetrics;
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
    private final ApplicationMetrics applicationMetrics;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.USERS)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.USERS_BY_ID, key = "#id")
    public UserResponse findById(Long id) {
        return userMapper.toResponse(findUser(id));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.USERS, allEntries = true)
    public UserResponse create(CreateUserRequest request) {
        String username = userMapper.normalizedUsername(request);
        if (userRepository.existsByUsername(username)) {
            log.atWarn()
                    .addKeyValue("event.action", "user_create")
                    .addKeyValue("event.outcome", "failure")
                    .addKeyValue("mentorship.resource", "user")
                    .log("Duplicate user creation");
            throw new DuplicateUsernameException(username);
        }

        User savedUser = userRepository.save(userMapper.toEntity(request));

        applicationMetrics.recordSuccessfulOperation(
                ApplicationMetrics.Resource.USER,
                ApplicationMetrics.Operation.CREATE
        );

        log.atInfo()
                .addKeyValue("event.action", "user_create")
                .addKeyValue("event.outcome", "success")
                .addKeyValue("mentorship.resource", "user")
                .addKeyValue("mentorship.entity_id", savedUser.getId())
                .log("User created");
        return userMapper.toResponse(savedUser);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USERS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.USERS_BY_ID, key = "#id")
    })
    public UserResponse update(Long id, UpdateUserRequest request) {
        var user = findUser(id);
        String username = userMapper.normalizedUsername(request);
        if (userRepository.existsByUsernameAndIdNot(username, id)) {
            log.atWarn()
                    .addKeyValue("event.action", "user_update")
                    .addKeyValue("event.outcome", "failure")
                    .addKeyValue("mentorship.resource", "user")
                    .addKeyValue("mentorship.entity_id", id)
                    .log("Duplicate user update");
            throw new DuplicateUsernameException(username);
        }

        userMapper.updateEntity(request, user);
        User savedUser = userRepository.save(user);

        applicationMetrics.recordSuccessfulOperation(
                ApplicationMetrics.Resource.USER,
                ApplicationMetrics.Operation.UPDATE
        );

        log.atInfo()
                .addKeyValue("event.action", "user_update")
                .addKeyValue("event.outcome", "success")
                .addKeyValue("mentorship.resource", "user")
                .addKeyValue("mentorship.entity_id", savedUser.getId())
                .log("User updated");
        return userMapper.toResponse(savedUser);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USERS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.USERS_BY_ID, key = "#id")
    })
    public void delete(Long id) {
        var user = findUser(id);
        if (taskRepository.existsByUserId(id)) {
            log.atWarn()
                    .addKeyValue("event.action", "user_delete")
                    .addKeyValue("event.outcome", "failure")
                    .addKeyValue("mentorship.resource", "user")
                    .addKeyValue("mentorship.entity_id", id)
                    .log("User deletion rejected");
            throw new UserHasAssignedTasksException(id);
        }

        userRepository.delete(user);

        applicationMetrics.recordSuccessfulOperation(
                ApplicationMetrics.Resource.USER,
                ApplicationMetrics.Operation.DELETE
        );

        log.atInfo()
                .addKeyValue("event.action", "user_delete")
                .addKeyValue("event.outcome", "success")
                .addKeyValue("mentorship.resource", "user")
                .addKeyValue("mentorship.entity_id", id)
                .log("User deleted");
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.atWarn()
                            .addKeyValue("event.action", "user_lookup")
                            .addKeyValue("event.outcome", "failure")
                            .addKeyValue("mentorship.resource", "user")
                            .addKeyValue("mentorship.entity_id", id)
                            .log("User not found");
                    return new ResourceNotFoundException("User not found: " + id);
                });
    }
}
