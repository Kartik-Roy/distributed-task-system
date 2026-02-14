package task.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeLoginResponseDto {
    private String accessToken;
    private LocalDateTime expiringOn;
}
