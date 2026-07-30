package org.nakrut.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nakrut.dto.CreateTaskRequest;
import org.nakrut.dto.TaskResponse;
import org.nakrut.mapper.TaskMapper;
import org.nakrut.model.Category;
import org.nakrut.model.Task;
import org.nakrut.model.TaskStatus;
import org.nakrut.model.User;
import org.nakrut.repository.TaskRepository;
import org.nakrut.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTests {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private TaskMapper taskMapper = new TaskMapper();

    @InjectMocks
    private TaskService taskService;

    @Test
    void returnsAllTasksWithRequestedStatus() {
        User user = new User("arseni");
        Task task = new Task("Learn Spring", "Build a CRUD API", Category.EDUCATION, user);
        task.setStatus(TaskStatus.DONE);
        when(taskRepository.findAllByStatus(TaskStatus.DONE)).thenReturn(List.of(task));

        List<TaskResponse> responses = taskService.findAllByStatus(TaskStatus.DONE);

        assertThat(responses)
                .extracting(TaskResponse::status)
                .containsExactly(TaskStatus.DONE);
        verify(taskRepository).findAllByStatus(TaskStatus.DONE);
    }

    @Test
    void createsTaskWithTodoStatus() {
        User user = new User("arseni");
        CreateTaskRequest request = new CreateTaskRequest(
                "Learn Spring",
                "Build a CRUD API",
                Category.EDUCATION,
                1L
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(request);

        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    }
}
