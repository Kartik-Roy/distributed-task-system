package task.server.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPublisher.class);
    private static final String TOPIC_PREFIX = "tasks.node.";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void publishTaskToNode(String nodeId, String taskId) {
        String topic = topicName(nodeId);

        kafkaTemplate.send(topic, taskId, taskId)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        Throwable root = ex;
                        while (root.getCause() != null) root = root.getCause();
                        log.error("Kafka publish failed. topic={}, key={}, value={}, root={}",
                                topic, taskId, taskId, root.toString(), ex);
                        return;
                    }
                    log.info("Kafka published. topic={}, partition={}, offset={}, key={}",
                            topic,
                            res.getRecordMetadata().partition(),
                            res.getRecordMetadata().offset(),
                            taskId);
                });

    }

    private String topicName(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId is required");
        }

        String safe = nodeId.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        return TOPIC_PREFIX + safe;
    }
}

