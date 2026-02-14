package task.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import task.server.entity.Task;
import task.server.entity.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Optional<Task> findByTaskIdAndAssignedNodeId(String taskId,String nodeId);
    Optional<Task> findByTaskId(String taskId);
    List<Task> findAllByAssignedNodeIdAndStatusNot(String nodeId, TaskStatus status);
    List<Task> findAllByAssignedNodeId(String nodeId);
    List<Task> findByStatusAndUpdatedOnBefore(TaskStatus status, LocalDateTime cutoff);

}
