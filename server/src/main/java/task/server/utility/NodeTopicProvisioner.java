package task.server.utility;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;
import task.server.entity.Node;
import task.server.repository.NodeRepository;

import java.util.List;

@Component
public class NodeTopicProvisioner {

    private static final String TOPIC_PREFIX = "tasks.node.";

    private final NodeRepository nodeRepository;
    private final KafkaAdmin kafkaAdmin;

    @Value("${app.kafka.nodeTopic.partitions:1}")
    private int partitions;

    @Value("${app.kafka.nodeTopic.replicationFactor:1}")
    private short replicationFactor;

    public NodeTopicProvisioner(NodeRepository nodeRepository, KafkaAdmin kafkaAdmin) {
        this.nodeRepository = nodeRepository;
        this.kafkaAdmin = kafkaAdmin;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createTopicsForActiveNodes() {
        List<Node> activeNodes = nodeRepository.findByIsActiveTrue();
        if (activeNodes.isEmpty()) return;

        List<NewTopic> topics = activeNodes.stream()
                .map(n -> new NewTopic(topicName(n.getNodeId()), partitions, replicationFactor))
                .toList();

         kafkaAdmin.createOrModifyTopics(topics.toArray(NewTopic[]::new));
    }

    private String topicName(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalStateException("Active node has empty nodeId");
        }
        String safe = nodeId.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        return TOPIC_PREFIX + safe;
    }
}

