package org.nakrut.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nakrut.dto.CreateTaskRequest;
import org.nakrut.dto.PageResponse;
import org.nakrut.dto.TaskResponse;
import org.nakrut.dto.UpdateTaskRequest;
import org.nakrut.exception.GlobalExceptionHandler;
import org.nakrut.exception.InvalidSortFieldException;
import org.nakrut.model.Category;
import org.nakrut.model.TaskStatus;
import org.nakrut.service.TaskService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TaskControllerTests {

    private static final LocalDate DUE_DATE = LocalDate.of(2026, 9, 10);

    @Mock
    private TaskService taskService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var pageableResolver = new PageableHandlerMethodArgumentResolver();
        pageableResolver.setMaxPageSize(100);

        mockMvc = MockMvcBuilders.standaloneSetup(new TaskController(taskService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(pageableResolver)
                .build();
    }

    @Test
    void returnsAllTasks() throws Exception {
        when(taskService.findAll(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pageResponse((TaskStatus.TODO)));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Learn Spring"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void returnsTasksWithRequestedStatus() throws Exception {
        when(taskService.findAll(
                eq(TaskStatus.IN_PROGRESS),
                isNull(),
                any(Pageable.class)
        )).thenReturn(pageResponse(TaskStatus.IN_PROGRESS));

        mockMvc.perform(get("/api/tasks").param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"));

        verify(taskService).findAll(
                eq(TaskStatus.IN_PROGRESS),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void returnsTasksWithRequestedDueDate() throws Exception {
        when(taskService.findAll(isNull(), eq(DUE_DATE), any(Pageable.class)))
                .thenReturn(pageResponse(TaskStatus.TODO));

        mockMvc.perform(get("/api/tasks").param("dueDate", "2026-09-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].dueDate").value("2026-09-10"));

        verify(taskService).findAll(isNull(), eq(DUE_DATE), any(Pageable.class));
    }

    @Test
    void combinesStatusAndDueDateFilters() throws Exception {
        when(taskService.findAll(
                eq(TaskStatus.IN_PROGRESS),
                eq(DUE_DATE),
                any(Pageable.class)
        )).thenReturn(pageResponse(TaskStatus.IN_PROGRESS));

        mockMvc.perform(get("/api/tasks")
                        .param("status", "IN_PROGRESS")
                        .param("dueDate", "2026-09-10"))
                .andExpect(status().isOk());

        verify(taskService).findAll(
                eq(TaskStatus.IN_PROGRESS),
                eq(DUE_DATE),
                any(Pageable.class)
        );
    }

    @Test
    void rejectsInvalidTaskStatus() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Parameter"))
                .andExpect(jsonPath("$.detail").value("Parameter 'status' has an invalid value"));
    }

    @Test
    void rejectsMalformedDueDate() throws Exception {
        mockMvc.perform(get("/api/tasks").param("dueDate", "10-09-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Parameter"))
                .andExpect(jsonPath("$.detail").value("Parameter 'dueDate' has an invalid value"));
    }

    @Test
    void rejectsUnsupportedTaskSortField() throws Exception {
        when(taskService.findAll(isNull(), isNull(), any(Pageable.class)))
                .thenThrow(new InvalidSortFieldException("description"));
        mockMvc.perform(get("/api/tasks")
                .param("sort", "description, asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Parameter"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Unsupported task sort field: description"));
    }

    @Test
    void returnsTaskById() throws Exception {
        when(taskService.findById(1L)).thenReturn(taskResponse(TaskStatus.TODO));

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void createsTask() throws Exception {
        when(taskService.create(any(CreateTaskRequest.class))).thenReturn(taskResponse(TaskStatus.TODO));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Learn Spring",
                                  "description":"Build a CRUD API",
                                  "dueDate":"2026-09-10",
                                  "category":"EDUCATION",
                                  "userId":1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void updatesTask() throws Exception {
        when(taskService.update(any(Long.class), any(UpdateTaskRequest.class)))
                .thenReturn(taskResponse(TaskStatus.IN_PROGRESS));

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Learn Spring",
                                  "description":"CRUD API completed",
                                  "status":"IN_PROGRESS",
                                  "dueDate":"2026-09-10",
                                  "category":"EDUCATION"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void deletesTask() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());

        verify(taskService).delete(1L);
    }

    @Test
    void forwardsPagingAndSortingWithCappedPageSize() throws Exception {
        when(taskService.findAll(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pageResponse(TaskStatus.TODO));

        mockMvc.perform(get("/api/tasks")
                        .param("page", "2")
                        .param("size", "500")
                        .param("sort", "status,desc")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk());

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskService).findAll(isNull(), isNull(), pageableCaptor.capture());

        var pageable = pageableCaptor.getValue();
        var orders = pageable.getSort().toList();

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(orders)
                .extracting(Sort.Order::getProperty)
                .containsExactly("status", "title");
        assertThat(orders)
                .extracting(Sort.Order::getDirection)
                .containsExactly(Sort.Direction.DESC, Sort.Direction.ASC);
    }

    private TaskResponse taskResponse(TaskStatus status) {
        return new TaskResponse(
                1L,
                "Learn Spring",
                "Build a CRUD API",
                status,
                DUE_DATE,
                Category.EDUCATION,
                1L
        );
    }

    private PageResponse<TaskResponse> pageResponse(TaskStatus status) {
        return new PageResponse<>(
                List.of(taskResponse(status)),
                0,
                20,
                1,
                1,
                true,
                true
        );
    }
}
