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
import org.nakrut.controller.dto.CreateTaskRequest;
import org.nakrut.controller.dto.TaskResponse;
import org.nakrut.controller.dto.UpdateTaskRequest;
import org.nakrut.model.Category;
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
        mockMvc = MockMvcBuilders.standaloneSetup(new TaskController(taskService)).build();
    }

    @Test
    void returnsAllTasks() throws Exception {
        when(taskService.findAll()).thenReturn(List.of(taskResponse(false)));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Learn Spring"));
    }

    @Test
    void returnsTaskById() throws Exception {
        when(taskService.findById(1L)).thenReturn(taskResponse(false));

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void createsTask() throws Exception {
        when(taskService.create(any(CreateTaskRequest.class))).thenReturn(taskResponse(false));

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
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void updatesTask() throws Exception {
        when(taskService.update(any(Long.class), any(UpdateTaskRequest.class)))
                .thenReturn(taskResponse(true));

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Learn Spring",
                                  "description":"CRUD API completed",
                                  "completed":true,
                                  "category":"EDUCATION"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void deletesTask() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());

        verify(taskService).delete(1L);
    }

    private TaskResponse taskResponse(boolean completed) {
        return new TaskResponse(
                1L,
                "Learn Spring",
                "Build a CRUD API",
                completed,
                Category.EDUCATION,
                1L
        );
    }
}
