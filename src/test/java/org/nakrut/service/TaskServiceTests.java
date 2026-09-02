package org.nakrut.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nakrut.dto.CreateTaskRequest;
import org.nakrut.dto.TaskResponse;
import org.nakrut.exception.InvalidSortFieldException;
import org.nakrut.mapper.TaskMapper;
import org.nakrut.model.Category;
import org.nakrut.model.Task;
import org.nakrut.model.TaskStatus;
import org.nakrut.model.User;
import org.nakrut.repository.TaskRepository;
import org.nakrut.repository.UserRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TaskServiceTests {

    private static final LocalDate DUE_DATE = LocalDate.of(2026, 9, 10);

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private TaskMapper taskMapper = new TaskMapper();

    @InjectMocks
    private TaskService taskService;

    @Test
    void returnPagedTasksWithWorkflowStatusSortAndIdTieBreaker(){
        var user = new User("arseni");
        var task = new Task(
                "Learn Spring",
                "Build a CRUD API",
                DUE_DATE,
                Category.EDUCATION,
                user
        );

        task.setStatus(TaskStatus.DONE);

        var requestedPageable = PageRequest.of(1, 5, Sort.by(Sort.Order.desc("status")));
        var repositoryPage = new PageImpl<>(List.of(task), PageRequest.of(1,5), 6);

        when(taskRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Task>>any(),
                any(Pageable.class)
        ))
                .thenReturn(repositoryPage);

        var response = taskService.findAll(TaskStatus.DONE, DUE_DATE, requestedPageable);

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<Task>>any(),
                pageableCaptor.capture()
        );

        var orders = pageableCaptor.getValue().getSort().toList();

        assertThat(orders)
                .extracting(Sort.Order::getProperty)
                .containsExactly("statusSortOrder", "id");
        assertThat(orders)
                .extracting(Sort.Order::getDirection)
                .containsExactly(Sort.Direction.DESC, Sort.Direction.ASC);

        assertThat(response.content())
                .extracting(TaskResponse::status)
                .containsExactly(TaskStatus.DONE);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(6);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.last()).isTrue();
    }

    @Test
    void createsTaskWithTodoStatus() {
        User user = new User("arseni");
        CreateTaskRequest request = new CreateTaskRequest(
                "Learn Spring",
                "Build a CRUD API",
                DUE_DATE,
                Category.EDUCATION,
                1L
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(request);

        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.dueDate()).isEqualTo(DUE_DATE);
    }

    @Test
    void rejectsUnsupportedTaskSortField() {
        var pageable = PageRequest.of(
                0,
                20,
                Sort.by("description")
        );

        assertThatThrownBy(() -> taskService.findAll(null, null, pageable))
                .isInstanceOf(InvalidSortFieldException.class)
                .hasMessage("Unsupported task sort field: description");
    }
}
