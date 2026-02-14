package task.worker.consumer;

import task.worker.dto.GetTaskDto;
import task.worker.utility.ServerApiClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import task.worker.executor.TaskExecutor;

@Component
public class TaskConsumer {

    private final ServerApiClient api;
    private final TaskExecutor executor;

    public TaskConsumer(ServerApiClient api, TaskExecutor executor) {
        this.api = api;
        this.executor = executor;
    }

    @KafkaListener(topics = "#{@nodeTopic}", concurrency = "1")
    public void onMessage(String taskId) {

        GetTaskDto task = api.getByTaskId(taskId);
        if (task == null || task.getTaskId()==null || task.getStatus().equals("completed") || task.getStatus().equals("timed_out")) return;

        if ("pending".equals(task.status)) {
            api.updateStatus(taskId, "pending", "in_progress");
        }

        try {
            String result = executor.execute(task.taskType, task.taskDetails);

            api.updateStatus(taskId, "in_progress", "completed");

        } catch (Exception e) {
            String old = "in_progress".equals(task.status) ? "in_progress" : "pending";
            api.updateStatus(taskId, old, "failed");
        }
    }
}
