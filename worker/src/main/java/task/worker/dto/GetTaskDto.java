package task.worker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetTaskDto {
    public String taskId;
    public String taskType;
    public String taskDetails;
    public String assignedNodeId;
    public String status;
}
