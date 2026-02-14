package task.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Task {

    @Id
    @Column(name = "task_id", length = 36, nullable = false, updatable = false)
    private String taskId;
    private String taskType;
    @Lob
    private String taskDetails;
    private String assignedNodeId;
    @Enumerated(EnumType.STRING)
    private TaskStatus status;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdOn;
    @UpdateTimestamp
    private LocalDateTime updatedOn;

    @PrePersist
    public void prePersist() {
        if (this.taskId == null || this.taskId.isBlank()) {
            this.taskId = UUID.randomUUID().toString();
        }
    }

}
