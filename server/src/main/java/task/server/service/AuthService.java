package task.server.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import task.server.dto.NodeLoginRequestDto;
import task.server.dto.NodeLoginResponseDto;
import task.server.dto.UserLoginRequestDto;
import task.server.dto.UserLoginResponseDto;
import task.server.entity.User;
import task.server.entity.Node;
import task.server.repository.UserRepository;
import task.server.repository.NodeRepository;
import task.server.security.JwtService;

import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {

    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${app.jwt.ttlSeconds}")
    private long ttlSeconds;


    public ResponseEntity<?> loginNode(NodeLoginRequestDto nodeLoginRequestDto) {
        String nodeId= nodeLoginRequestDto.getNodeId();
        String nodeSecret=nodeLoginRequestDto.getNodeSecret();

        if (nodeId == null || nodeId.isBlank() || nodeSecret == null || nodeSecret.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
        }

        Node node = nodeRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid credentials"));

        if (!node.isActive()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Node disabled");
        }

        // IMPORTANT: nodeSecretHash must exist in DB (BCrypt hash)
        if (!encoder.matches(nodeSecret, node.getNodeSecretHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
        }
        NodeLoginResponseDto nodeLoginResponseDto=new NodeLoginResponseDto(jwtService.mintNodeToken(node.getNodeId()), LocalDateTime.now().plusSeconds(ttlSeconds-60));
        return ResponseEntity.status(HttpStatus.OK).body(nodeLoginResponseDto);
    }

    public ResponseEntity<?> login(UserLoginRequestDto userLoginRequestDto) {
        User user = userRepository.findByUsername(userLoginRequestDto.getUsername())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid credentials"));

        if (!user.isActive()) throw new ResponseStatusException(UNAUTHORIZED, "Disabled");
        if (!encoder.matches(userLoginRequestDto.getPassword(), user.getPasswordHash()))
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");

        UserLoginResponseDto userLoginResponseDto=new UserLoginResponseDto(jwtService.mintUserToken(user.getId().toString(),user.getRole()),user.getRole());

        return ResponseEntity.status(HttpStatus.OK).body(userLoginResponseDto);

    }
}

