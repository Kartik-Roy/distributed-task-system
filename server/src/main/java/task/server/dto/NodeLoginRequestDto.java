package task.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeLoginRequestDto {
    private String nodeId;
    private String nodeSecret;
}
