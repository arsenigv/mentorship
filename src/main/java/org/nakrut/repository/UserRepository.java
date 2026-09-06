package org.nakrut.repository;

import org.nakrut.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Override
    @Query(value = "SELECT * FROM app_users WHERE id = :id", nativeQuery = true)
    Optional<User> findById(@Param("id") Long id);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);
}
