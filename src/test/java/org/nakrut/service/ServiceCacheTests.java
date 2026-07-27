package org.nakrut.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nakrut.dto.CreateTaskRequest;
import org.nakrut.dto.CreateUserRequest;
import org.nakrut.dto.TaskResponse;
import org.nakrut.dto.UpdateTaskRequest;
import org.nakrut.dto.UpdateUserRequest;
import org.nakrut.dto.UserResponse;
import org.nakrut.mapper.TaskMapper;
import org.nakrut.mapper.UserMapper;
import org.nakrut.model.Category;
import org.nakrut.model.Task;
import org.nakrut.model.TaskStatus;
import org.nakrut.model.User;
import org.nakrut.repository.TaskRepository;
import org.nakrut.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ServiceCacheTests.CacheConfiguration.class)
class ServiceCacheTests {

    @Autowired
    private UserService userService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private CacheManager cacheManager;

    private User user;
    private Task task;
    private UserResponse userResponse;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        reset(userRepository, taskRepository, userMapper, taskMapper);
        cacheManager.getCacheNames().forEach(name -> cache(name).clear());

        user = new User("arseni");
        task = new Task("Learn Spring", "Build a CRUD API", Category.EDUCATION, user);
        userResponse = new UserResponse(1L, "arseni");
        taskResponse = new TaskResponse(
                1L,
                "Learn Spring",
                "Build a CRUD API",
                TaskStatus.TODO,
                Category.EDUCATION,
                1L
        );

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toEntity(new CreateUserRequest("arseni"))).thenReturn(user);
        when(userMapper.normalizedUsername(new CreateUserRequest("arseni"))).thenReturn("arseni");
        when(userMapper.normalizedUsername(new UpdateUserRequest("updated"))).thenReturn("updated");
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        when(taskRepository.findAll()).thenReturn(List.of(task));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toEntity(
                new CreateTaskRequest("Learn Spring", "Build a CRUD API", Category.EDUCATION, 1L),
                user
        )).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);
    }

    @Test
    void cachesUserAndTaskListAndDetailReads() {
        assertThat(userService.findAll()).containsExactly(userResponse);
        assertThat(userService.findAll()).containsExactly(userResponse);
        assertThat(userService.findById(1L)).isEqualTo(userResponse);
        assertThat(userService.findById(1L)).isEqualTo(userResponse);

        assertThat(taskService.findAll()).containsExactly(taskResponse);
        assertThat(taskService.findAll()).containsExactly(taskResponse);
        assertThat(taskService.findById(1L)).isEqualTo(taskResponse);
        assertThat(taskService.findById(1L)).isEqualTo(taskResponse);

        verify(userRepository, times(1)).findAll();
        verify(userRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).findAll();
        verify(taskRepository, times(1)).findById(1L);
    }

    @Test
    void createEvictsCorrespondingListCache() {
        cache("users").put(SimpleKey.EMPTY, List.of(userResponse));
        cache("tasks").put(SimpleKey.EMPTY, List.of(taskResponse));

        userService.create(new CreateUserRequest("arseni"));
        taskService.create(new CreateTaskRequest(
                "Learn Spring",
                "Build a CRUD API",
                Category.EDUCATION,
                1L
        ));

        assertThat(cache("users").get(SimpleKey.EMPTY)).isNull();
        assertThat(cache("tasks").get(SimpleKey.EMPTY)).isNull();
    }

    @Test
    void updateEvictsCorrespondingListAndDetailCaches() {
        populateCaches();

        userService.update(1L, new UpdateUserRequest("updated"));
        taskService.update(1L, new UpdateTaskRequest(
                "Updated task",
                "Updated description",
                TaskStatus.IN_PROGRESS,
                Category.EDUCATION
        ));

        assertCachesAreEmpty();
    }

    @Test
    void deleteEvictsCorrespondingListAndDetailCaches() {
        populateCaches();

        userService.delete(1L);
        taskService.delete(1L);

        assertCachesAreEmpty();
    }

    private void populateCaches() {
        cache("users").put(SimpleKey.EMPTY, List.of(userResponse));
        cache("usersById").put(1L, userResponse);
        cache("tasks").put(SimpleKey.EMPTY, List.of(taskResponse));
        cache("tasksById").put(1L, taskResponse);
    }

    private void assertCachesAreEmpty() {
        assertThat(cache("users").get(SimpleKey.EMPTY)).isNull();
        assertThat(cache("usersById").get(1L)).isNull();
        assertThat(cache("tasks").get(SimpleKey.EMPTY)).isNull();
        assertThat(cache("tasksById").get(1L)).isNull();
    }

    private Cache cache(String name) {
        return cacheManager.getCache(name);
    }

    @Configuration
    @EnableCaching
    static class CacheConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("users", "usersById", "tasks", "tasksById");
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        TaskRepository taskRepository() {
            return mock(TaskRepository.class);
        }

        @Bean
        UserMapper userMapper() {
            return mock(UserMapper.class);
        }

        @Bean
        TaskMapper taskMapper() {
            return mock(TaskMapper.class);
        }

        @Bean
        UserService userService(
                UserRepository userRepository,
                TaskRepository taskRepository,
                UserMapper userMapper
        ) {
            return new UserService(userRepository, taskRepository, userMapper);
        }

        @Bean
        TaskService taskService(
                TaskRepository taskRepository,
                UserRepository userRepository,
                TaskMapper taskMapper
        ) {
            return new TaskService(taskRepository, userRepository, taskMapper);
        }
    }
}
