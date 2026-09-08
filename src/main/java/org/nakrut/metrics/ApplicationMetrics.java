package org.nakrut.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.nakrut.model.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

@Component
public class ApplicationMetrics {

    private static final String BUSINESS_OPERATIONS = "mentorship.business.operations";
    private static final String TASK_STATUS_ASSIGNMENTS = "mentorship.task.status.assignments";

    private final Map<Resource, Map<Operation, Counter>> businessOperationCounters;
    private final Map<TaskStatus, Counter> taskStatusAssignmentCounters;

    public ApplicationMetrics(MeterRegistry meterRegistry) {
        businessOperationCounters = registerBusinessOperationCounters(meterRegistry);
        taskStatusAssignmentCounters = registerTaskStatusAssignmentCounters(meterRegistry);
    }

    public void recordSuccessfulOperation(Resource resource, Operation operation) {
        businessOperationCounters.get(resource).get(operation).increment();
    }

    public void recordTaskStatusAssignment(TaskStatus status) {
        taskStatusAssignmentCounters.get(status).increment();
    }

    private Map<Resource, Map<Operation, Counter>> registerBusinessOperationCounters(
            MeterRegistry meterRegistry
    ) {
        var counters = new EnumMap<Resource, Map<Operation, Counter>>(Resource.class);

        for (Resource resource : Resource.values()) {
            var operationCounters = new EnumMap<Operation, Counter>(Operation.class);
            for (Operation operation : Operation.values()) {
                operationCounters.put(
                        operation,
                        Counter.builder(BUSINESS_OPERATIONS)
                                .description("Number of successful business operations")
                                .tag("resource", resource.name().toLowerCase(Locale.ROOT))
                                .tag("operation", operation.name().toLowerCase(Locale.ROOT))
                                .register(meterRegistry)
                );
            }
            counters.put(resource, operationCounters);
        }

        return counters;
    }

    private Map<TaskStatus, Counter> registerTaskStatusAssignmentCounters(MeterRegistry meterRegistry) {
        var counters = new EnumMap<TaskStatus, Counter>(TaskStatus.class);

        for (TaskStatus status : TaskStatus.values()) {
            counters.put(
                    status,
                    Counter.builder(TASK_STATUS_ASSIGNMENTS)
                            .description("Number of task status assignments")
                            .tag("status", status.name())
                            .register(meterRegistry)
            );
        }

        return counters;
    }

    public enum Resource {
        USER,
        TASK
    }

    public enum Operation {
        CREATE,
        UPDATE,
        DELETE
    }
}
