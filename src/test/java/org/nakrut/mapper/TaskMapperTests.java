package org.nakrut.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.nakrut.dto.CreateTaskRequest;
import org.nakrut.dto.TaskResponse;
import org.nakrut.dto.UpdateTaskRequest;
import org.nakrut.model.Category;
import org.nakrut.model.Task;
import org.nakrut.model.TaskStatus;
import org.nakrut.model.User;
import org.springframework.test.util.ReflectionTestUtils;

class TaskMapperTests {

    private static final LocalDate DUE_DATE = LocalDate.of(2026, 9, 10);

    private final TaskMapper taskMapper = new TaskMapper();

    @Test
    void mapsCreateRequestToEntityWithTodoStatus() {
        User user = new User("arseni");
        CreateTaskRequest request = new CreateTaskRequest(
                "Learn Spring",
                "Build a CRUD API",
                DUE_DATE,
                Category.EDUCATION,
                1L
        );

        Task task = taskMapper.toEntity(request, user);

        assertThat(task.getTitle()).isEqualTo("Learn Spring");
        assertThat(task.getDescription()).isEqualTo("Build a CRUD API");
        assertThat(task.getCategory()).isEqualTo(Category.EDUCATION);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getDueDate()).isEqualTo(DUE_DATE);
        assertThat(task.getUser()).isSameAs(user);
    }

    @Test
    void updatesMutableTaskFieldsWithoutChangingOwner() {
        User user = new User("arseni");
        Task task = new Task("Old title", "Old description", DUE_DATE.minusDays(1), Category.HOME, user);
        UpdateTaskRequest request = new UpdateTaskRequest(
                "New title",
                "New description",
                TaskStatus.IN_PROGRESS,
                DUE_DATE,
                Category.WORK
        );

        taskMapper.updateEntity(request, task);

        assertThat(task.getTitle()).isEqualTo("New title");
        assertThat(task.getDescription()).isEqualTo("New description");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getDueDate()).isEqualTo(DUE_DATE);
        assertThat(task.getCategory()).isEqualTo(Category.WORK);
        assertThat(task.getUser()).isSameAs(user);
    }

    @Test
    void mapsEntityToResponseWithUserId() {
        User user = new User("arseni");
        Task task = new Task("Learn Spring", "Build a CRUD API", DUE_DATE, Category.EDUCATION, user);
        task.setStatus(TaskStatus.DONE);
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(task, "id", 2L);

        TaskResponse response = taskMapper.toResponse(task);

        assertThat(response).isEqualTo(new TaskResponse(
                2L,
                "Learn Spring",
                "Build a CRUD API",
                TaskStatus.DONE,
                DUE_DATE,
                Category.EDUCATION,
                1L
        ));
    }
}
