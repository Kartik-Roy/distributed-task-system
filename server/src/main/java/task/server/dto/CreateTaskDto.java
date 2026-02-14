package task.server.dto;

import lombok.Data;

@Data
public class CreateTaskDto {
    private String taskType;
    private String taskDetails;
    private String assignedNodeId;
}
