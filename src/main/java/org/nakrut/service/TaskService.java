package org.nakrut.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nakrut.config.CacheNames;
import org.nakrut.dto.CreateTaskRequest;
import org.nakrut.dto.PageResponse;
import org.nakrut.dto.TaskResponse;
import org.nakrut.dto.UpdateTaskRequest;
import org.nakrut.exception.InvalidSortFieldException;
import org.nakrut.exception.ResourceNotFoundException;
import org.nakrut.mapper.TaskMapper;
import org.nakrut.model.Task;
import org.nakrut.model.TaskStatus;
import org.nakrut.model.User;
import org.nakrut.repository.TaskRepository;
import org.nakrut.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "title",
            "status"
    );

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.TASKS)
    public PageResponse<TaskResponse> findAll(
            TaskStatus status,
            LocalDate dueDate,
        Pageable pageable
    ) {
        var normalizedPageable = normalizePageable(pageable);
        var tasks = taskRepository.findAll(taskFilters(status, dueDate), normalizedPageable)
                .map(taskMapper::toResponse);

        return PageResponse.from(tasks);
    }

    private Specification<Task> taskFilters(TaskStatus status, LocalDate dueDate) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (dueDate != null) {
                predicates.add(criteriaBuilder.equal(root.get("dueDate"), dueDate));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.TASKS_BY_ID, key = "#id")
    public TaskResponse findById(Long id) {
        return taskMapper.toResponse(findTask(id));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.TASKS, allEntries = true)
    public TaskResponse create(CreateTaskRequest request) {
        User user = findUser(request.userId());
        Task task = taskMapper.toEntity(request, user);
        Task savedTask = taskRepository.save(task);
        log.info("Task created: id={}, userId={}", savedTask.getId(), request.userId());
        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.TASKS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.TASKS_BY_ID, key = "#id")
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
            @CacheEvict(cacheNames = CacheNames.TASKS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.TASKS_BY_ID, key = "#id")
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

    private Pageable normalizePageable(Pageable pageable) {
        var orders = new ArrayList<Sort.Order>();
        var containsIdSort = false;

        for(var order : pageable.getSort()) {
            var property = order.getProperty();

            if (!ALLOWED_SORT_FIELDS.contains(property)) {
                throw new InvalidSortFieldException(property);
            }

            if("id".equals(property)) {
                containsIdSort = true;
            }

            var mappedProperty = "status".equals(property)
                    ? "statusSortOrder"
                    : property;

            orders.add(order.withProperty(mappedProperty));
        }

        if(!containsIdSort) {
            orders.add(Sort.Order.asc("id"));
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(orders)
        );
    }

}
