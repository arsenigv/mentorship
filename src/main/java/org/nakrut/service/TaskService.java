package org.nakrut.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nakrut.dto.CreateTaskRequest;
import org.nakrut.dto.TaskResponse;
import org.nakrut.dto.UpdateTaskRequest;
import org.nakrut.exception.ResourceNotFoundException;
import org.nakrut.mapper.TaskMapper;
import org.nakrut.model.Task;
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
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "tasks")
    public List<TaskResponse> findAll() {
        return taskRepository.findAll().stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "tasksById", key = "#id")
    public TaskResponse findById(Long id) {
        return taskMapper.toResponse(findTask(id));
    }

    @Transactional
    @CacheEvict(cacheNames = "tasks", allEntries = true)
    public TaskResponse create(CreateTaskRequest request) {
        User user = findUser(request.userId());
        Task task = taskMapper.toEntity(request, user);
        Task savedTask = taskRepository.save(task);
        log.info("Task created: id={}, userId={}", savedTask.getId(), request.userId());
        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "tasks", allEntries = true),
            @CacheEvict(cacheNames = "tasksById", key = "#id")
    })
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = findTask(id);
        taskMapper.updateEntity(request, task);
        Task savedTask = taskRepository.save(task);
        log.info("Task updated: id={}, status={}", savedTask.getId(), savedTask.getStatus());
        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "tasks", allEntries = true),
            @CacheEvict(cacheNames = "tasksById", key = "#id")
    })
    public void delete(Long id) {
        Task task = findTask(id);
        taskRepository.delete(task);
        log.info("Task deleted: id={}", id);
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Task not found: id={}", id);
                    return new ResourceNotFoundException("Task not found: " + id);
                });
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Task owner not found: userId={}", id);
                    return new ResourceNotFoundException("User not found: " + id);
                });
    }

}
