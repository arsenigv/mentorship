package org.nakrut.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nakrut.dto.CreateTaskRequest;
import org.nakrut.dto.TaskResponse;
import org.nakrut.dto.UpdateTaskRequest;
import org.nakrut.exception.GlobalExceptionHandler;
import org.nakrut.model.Category;
import org.nakrut.model.TaskStatus;
import org.nakrut.service.TaskService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TaskControllerTests {

    @Mock
    private TaskService taskService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TaskController(taskService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsAllTasks() throws Exception {
        when(taskService.findAll()).thenReturn(List.of(taskResponse(TaskStatus.TODO)));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Learn Spring"));
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

    private TaskResponse taskResponse(TaskStatus status) {
        return new TaskResponse(
                1L,
                "Learn Spring",
                "Build a CRUD API",
                status,
                Category.EDUCATION,
                1L
        );
    }
}
