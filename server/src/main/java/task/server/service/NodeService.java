package task.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import task.server.dto.CreateNodeDto;
import task.server.entity.Node;
import task.server.repository.NodeRepository;

import java.time.LocalDateTime;

@Service
public class NodeService {

    @Autowired
    private NodeRepository nodeRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public ResponseEntity<?> create(CreateNodeDto createNodeDto){
        if(nodeRepository.findByNodeId(createNodeDto.getNodeId()).isPresent())
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("NodeId already taken");

        Node node=Node.builder()
                .nodeId(createNodeDto.getNodeId())
                .createdOn(LocalDateTime.now())
                .isActive(true)
                .nodeSecretHash(encoder.encode(createNodeDto.getNodeSecretHash()))
                .build();

        nodeRepository.save(node);

        return ResponseEntity.status(HttpStatus.OK).body(node);
    }

}
