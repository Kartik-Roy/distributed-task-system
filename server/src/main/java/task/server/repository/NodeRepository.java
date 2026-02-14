package task.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import task.server.entity.Node;

import java.util.List;
import java.util.Optional;

public interface NodeRepository extends JpaRepository<Node, Long> {
    List<Node> findByIsActiveTrue();
    Optional<Node> findByNodeId(String nodeId);
}
