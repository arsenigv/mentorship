package org.nakrut.repository;

import org.nakrut.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    boolean existsByUserId(Long userId);
}
