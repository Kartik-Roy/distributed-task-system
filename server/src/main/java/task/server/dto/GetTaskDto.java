package task.server.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import task.server.entity.TaskStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetTaskDto {
    private String taskId;
    private String taskType;
    private String taskDetails;
    private String assignedNodeId;
    private String status;
}
