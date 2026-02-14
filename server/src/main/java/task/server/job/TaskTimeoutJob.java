package task.server.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import task.server.entity.Task;
import task.server.entity.TaskStatus;
import task.server.repository.TaskRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskTimeoutJob {

    @Autowired
    private TaskRepository taskRepository;

    @Value("${app.task.timeoutSeconds}")
    private long timeoutSeconds;

    public TaskTimeoutJob(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Scheduled(cron="0 * * * * *")
    public void markTimedOutTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);

        List<Task> stuckList = taskRepository.findByStatusAndUpdatedOnBefore(TaskStatus.in_progress, cutoff);

        for (Task task : stuckList) {
            task.setStatus(TaskStatus.timed_out);
            task.setUpdatedOn(LocalDateTime.now());
            taskRepository.save(task);
        }
    }
}

