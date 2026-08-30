package org.nakrut;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"dev", "no-cache"})
class MentorshipApplicationTests {

    @Test
    void contextLoads() {
    }
}
