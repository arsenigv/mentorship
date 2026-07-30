package org.nakrut.repository;

import java.util.List;
import org.nakrut.model.Task;
import org.nakrut.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByStatus(TaskStatus status);

    boolean existsByUserId(Long userId);
}
