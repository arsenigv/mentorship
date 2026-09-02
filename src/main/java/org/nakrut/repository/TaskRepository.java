package org.nakrut.repository;

import java.util.List;
import org.nakrut.model.Task;
import org.nakrut.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findAllByStatus(TaskStatus status, Pageable pageable);

    boolean existsByUserId(Long userId);
}
