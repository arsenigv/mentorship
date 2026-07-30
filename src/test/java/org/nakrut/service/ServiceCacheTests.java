package org.nakrut.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.nakrut.config.CacheNames.TASKS;
import static org.nakrut.config.CacheNames.TASKS_BY_ID;
import static org.nakrut.config.CacheNames.USERS;
import static org.nakrut.config.CacheNames.USERS_BY_ID;

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
import org.springframework.test.util.ReflectionTestUtils;

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
    private CacheManager cacheManager;

    private User user;
    private Task task;
    private UserResponse userResponse;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        reset(userRepository);
        reset(taskRepository);
        cacheManager.getCacheNames().forEach(name -> cache(name).clear());

        user = new User("arseni");
        task = new Task("Learn Spring", "Build a CRUD API", Category.EDUCATION, user);
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(task, "id", 1L);
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
        doAnswer(invocation -> invocation.getArgument(0, User.class))
                .when(userRepository).save(any(User.class));

        when(taskRepository.findAll()).thenReturn(List.of(task));
        when(taskRepository.findAllByStatus(TaskStatus.TODO)).thenReturn(List.of(task));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        doAnswer(invocation -> invocation.getArgument(0, Task.class))
                .when(taskRepository).save(any(Task.class));
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
    void cachesTaskListsByStatus() {
        assertThat(taskService.findAllByStatus(TaskStatus.TODO)).containsExactly(taskResponse);
        assertThat(taskService.findAllByStatus(TaskStatus.TODO)).containsExactly(taskResponse);

        verify(taskRepository, times(1)).findAllByStatus(TaskStatus.TODO);
    }

    @Test
    void createEvictsCorrespondingListCache() {
        cache(USERS).put(SimpleKey.EMPTY, List.of(userResponse));
        cache(TASKS).put(SimpleKey.EMPTY, List.of(taskResponse));

        userService.create(new CreateUserRequest("arseni"));
        taskService.create(new CreateTaskRequest(
                "Learn Spring",
                "Build a CRUD API",
                Category.EDUCATION,
                1L
        ));

        assertThat(cache(USERS).get(SimpleKey.EMPTY)).isNull();
        assertThat(cache(TASKS).get(SimpleKey.EMPTY)).isNull();
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
        cache(USERS).put(SimpleKey.EMPTY, List.of(userResponse));
        cache(USERS_BY_ID).put(1L, userResponse);
        cache(TASKS).put(SimpleKey.EMPTY, List.of(taskResponse));
        cache(TASKS_BY_ID).put(1L, taskResponse);
    }

    private void assertCachesAreEmpty() {
        assertThat(cache(USERS).get(SimpleKey.EMPTY)).isNull();
        assertThat(cache(USERS_BY_ID).get(1L)).isNull();
        assertThat(cache(TASKS).get(SimpleKey.EMPTY)).isNull();
        assertThat(cache(TASKS_BY_ID).get(1L)).isNull();
    }

    private Cache cache(String name) {
        return cacheManager.getCache(name);
    }

    @Configuration
    @EnableCaching
    static class CacheConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(USERS, USERS_BY_ID, TASKS, TASKS_BY_ID);
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
            return new UserMapper();
        }

        @Bean
        TaskMapper taskMapper() {
            return new TaskMapper();
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
