package org.nakrut.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nakrut.dto.CreateUserRequest;
import org.nakrut.dto.UserResponse;
import org.nakrut.exception.DuplicateUsernameException;
import org.nakrut.exception.UserHasAssignedTasksException;
import org.nakrut.model.User;
import org.nakrut.repository.TaskRepository;
import org.nakrut.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createsUserWithTrimmedUsername() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.create(new CreateUserRequest("  arseni  "));

        assertThat(response.username()).isEqualTo("arseni");
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userRepository.existsByUsername("arseni")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(new CreateUserRequest("arseni")))
                .isInstanceOf(DuplicateUsernameException.class)
                .hasMessage("Username already exists: arseni");
    }

    @Test
    void rejectsDeletingUserWithAssignedTasks() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User("arseni")));
        when(taskRepository.existsByUserId(1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.delete(1L))
                .isInstanceOf(UserHasAssignedTasksException.class)
                .hasMessage("Cannot delete user with assigned tasks: 1");
    }
}
