package org.nakrut.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.nakrut.metrics.ApplicationMetrics.Operation;
import org.nakrut.metrics.ApplicationMetrics.Resource;
import org.nakrut.model.TaskStatus;

public class ApplicationMetricsTests {

    @Test
    void registersBusinessMetricsBeforeTheFirstOperation() {
        var registry = new SimpleMeterRegistry();

        new ApplicationMetrics(registry);

        assertThat(registry.find("mentorship.business.operations").counters()).hasSize(6);
        assertThat(registry.find("mentorship.task.status.assignments").counters()).hasSize(3);
        assertThat(registry
                .get("mentorship.business.operations")
                .tags("resource", "user", "operation", "create")
                .counter()
                .count()).isZero();
    }

    @Test
    void recordSuccessfulBusinessOperation() {
        var registry = new SimpleMeterRegistry();
        var metrics = new ApplicationMetrics(registry);

        metrics.recordSuccessfulOperation(Resource.USER, Operation.CREATE);

        double count = registry
                .get("mentorship.business.operations")
                .tags("resource", "user", "operation", "create")
                .counter()
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void recordsTaskStatusAssignment() {
        var registry = new SimpleMeterRegistry();
        var metrics = new ApplicationMetrics(registry);

        metrics.recordTaskStatusAssignment(TaskStatus.IN_PROGRESS);

        double count = registry
                .get("mentorship.task.status.assignments")
                .tag("status", "IN_PROGRESS")
                .counter()
                .count();

        assertThat(count).isEqualTo(1);
    }
}
