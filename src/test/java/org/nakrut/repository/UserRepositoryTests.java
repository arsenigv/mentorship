package org.nakrut.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.nakrut.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.cache.type=none")
@ActiveProfiles("dev")
@Transactional
class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findsUserById() {
        var username = "native-query-" + UUID.randomUUID();
        var savedUser = userRepository.saveAndFlush(new User(username));
        entityManager.clear();

        var result = userRepository.findById(savedUser.getId());

        assertThat(result)
                .isPresent()
                .get()
                .extracting(User::getUsername)
                .isEqualTo(username);
    }
}
