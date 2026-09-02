package org.nakrut.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest(properties = "spring.cache.type=none")
@ActiveProfiles("dev")
public class TaskPagingIntegrationTests {

    @Autowired
    private TaskService taskService;

    @Test
    void sortsTasksByWorkflowStatus(){
        var pageable = PageRequest.of(
                0,
                100,
                Sort.by(Sort.Order.asc("status"))
        );

        var response = taskService.findAll(pageable);

        var statusRanks = response.content().stream()
                .map(task -> switch (task.status()) {
                    case TODO -> 0;
                    case IN_PROGRESS -> 1;
                    case DONE -> 2;
                })
                .toList();

        assertThat(statusRanks)
                .isNotEmpty()
                .isSorted();
    }

}
