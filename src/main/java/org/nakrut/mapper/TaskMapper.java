package org.nakrut.mapper;

import org.nakrut.dto.CreateTaskRequest;
import org.nakrut.dto.TaskResponse;
import org.nakrut.dto.UpdateTaskRequest;
import org.nakrut.model.Task;
import org.nakrut.model.User;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toEntity(CreateTaskRequest request, User user) {
        return new Task(
                request.title(),
                request.description(),
                request.dueDate(),
                request.category(),
                user
        );
    }

    public void updateEntity(UpdateTaskRequest request, Task task) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setDueDate(request.dueDate());
        task.setCategory(request.category());
    }

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                task.getCategory(),
                task.getUser().getId()
        );
    }
}
