package org.nakrut.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nakrut.controller.TaskController;
import org.nakrut.controller.UserController;
import org.nakrut.dto.CreateUserRequest;
import org.nakrut.service.TaskService;
import org.nakrut.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTests {

    @Mock
    private TaskService taskService;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TaskController(taskService), new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsProblemDetailWhenResourceIsNotFound() throws Exception {
        when(taskService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Task not found: 99"));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Task not found: 99"))
                .andExpect(jsonPath("$.instance").value("/api/tasks/99"));
    }

    @Test
    void returnsConflictForDuplicateUsername() throws Exception {
        when(userService.create(any(CreateUserRequest.class)))
                .thenThrow(new DuplicateUsernameException("arseni"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"arseni"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.detail").value("Username already exists: arseni"));
    }

    @Test
    void returnsConflictWhenUserHasAssignedTasks() throws Exception {
        doThrow(new UserHasAssignedTasksException(1L)).when(userService).delete(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cannot delete user with assigned tasks: 1"));
    }

    @Test
    void returnsFieldErrorsForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s",
                                  "description":"Build a CRUD API",
                                  "category":"EDUCATION",
                                  "userId":0
                                }
                                """.formatted("a".repeat(256))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors[?(@.field == 'title')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field == 'userId')]").isNotEmpty());
    }

    @Test
    void returnsBadRequestForInvalidEnumValue() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Learn Spring",
                                  "category":"INVALID",
                                  "userId":1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed Request"))
                .andExpect(jsonPath("$.detail").value("Request body is missing or malformed"));
    }

    @Test
    void returnsBadRequestForInvalidPathVariable() throws Exception {
        mockMvc.perform(get("/api/tasks/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Parameter"))
                .andExpect(jsonPath("$.detail").value("Parameter 'id' has an invalid value"));
    }

    @Test
    void preservesProblemDetailForFrameworkErrors() throws Exception {
        mockMvc.perform(post("/api/tasks/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void sanitizesDatabaseConstraintFailures() throws Exception {
        when(userService.create(any(CreateUserRequest.class)))
                .thenThrow(new DataIntegrityViolationException("private database message"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"arseni"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Data Integrity Conflict"))
                .andExpect(jsonPath("$.detail").value("Request conflicts with existing data"))
                .andExpect(content().string(not(containsString("private database message"))));
    }

    @Test
    void sanitizesUnexpectedFailures() throws Exception {
        when(taskService.findById(1L)).thenThrow(new RuntimeException("private internal message"));

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("private internal message"))));
    }
}
