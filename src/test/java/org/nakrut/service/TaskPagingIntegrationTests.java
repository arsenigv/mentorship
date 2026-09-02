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

        var response = taskService.findAll(null, null, pageable);

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

    @Test
    void filtersTasksByStatusAndDueDate() {
        var pageable = PageRequest.of(0, 100, Sort.by(Sort.Order.asc("id")));
        var sample = taskService.findAll(null, null, pageable).content().getFirst();

        var response = taskService.findAll(sample.status(), sample.dueDate(), pageable);

        assertThat(response.content())
                .isNotEmpty()
                .allSatisfy(task -> {
                    assertThat(task.status()).isEqualTo(sample.status());
                    assertThat(task.dueDate()).isEqualTo(sample.dueDate());
                });
        assertThat(response.content())
                .extracting(task -> task.id())
                .contains(sample.id());
    }

}
