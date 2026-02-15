package task.server.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateNodeDto {
    private String nodeId;
    private String nodeSecretHash;
}
