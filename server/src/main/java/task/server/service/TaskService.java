package task.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import task.server.dto.CreateTaskDto;
import task.server.dto.GetTaskDto;
import task.server.entity.Task;
import task.server.entity.TaskStatus;
import task.server.producer.KafkaPublisher;
import task.server.repository.NodeRepository;
import task.server.repository.TaskRepository;
import task.server.security.NodeAuthentication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private KafkaPublisher kafkaPublisher;
    @Autowired
    private NodeRepository nodeRepository;

    public ResponseEntity<?> createTask(CreateTaskDto createTaskDto){
        if(nodeRepository.findByNodeId(createTaskDto.getAssignedNodeId()).isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Node does not exist");

        Task task=Task.builder()
                .taskType(createTaskDto.getTaskType())
                .taskDetails(createTaskDto.getTaskDetails())
                .assignedNodeId(createTaskDto.getAssignedNodeId())
                .status(TaskStatus.pending)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        task=taskRepository.save(task);

        kafkaPublisher.publishTaskToNode(task.getAssignedNodeId(),task.getTaskId());

        return ResponseEntity.status(HttpStatus.OK).body(task);
    }

    public ResponseEntity<?> getTaskByTaskId(String taskId){
        String nodeId = ((NodeAuthentication) SecurityContextHolder.getContext().getAuthentication()).nodeId();

        Task task=taskRepository.findByTaskIdAndAssignedNodeId(taskId, nodeId).orElse(null);
        if(task==null)
            return ResponseEntity.status(HttpStatus.OK).body(new GetTaskDto());

        GetTaskDto getTaskDto= GetTaskDto.builder()
                .taskId(task.getTaskId())
                .taskType(task.getTaskType())
                .taskDetails(task.getTaskDetails())
                .assignedNodeId(task.getAssignedNodeId())
                .status(task.getStatus().name())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(getTaskDto);
    }

    public ResponseEntity<?> getAllTaskForNode(){
        String nodeId = ((NodeAuthentication) SecurityContextHolder.getContext().getAuthentication()).nodeId();
        List<Task> taskList=taskRepository.findAllByAssignedNodeId(nodeId);

        List<GetTaskDto> getTaskDtoList=new ArrayList<>();

        taskList.forEach(task->{
            GetTaskDto getTaskDto= GetTaskDto.builder()
                    .taskId(task.getTaskId())
                    .taskType(task.getTaskType())
                    .taskDetails(task.getTaskDetails())
                    .assignedNodeId(task.getAssignedNodeId())
                    .status(task.getStatus().name())
                    .build();
            getTaskDtoList.add(getTaskDto);
        });



        return ResponseEntity.status(HttpStatus.OK).body(getTaskDtoList);
    }

    public ResponseEntity<?> reassignTaskToDifferentNode(String taskId, String nodeId){
        Task task=taskRepository.findByTaskId(taskId).orElseThrow();
        task.setStatus(TaskStatus.pending);
        task.setAssignedNodeId(nodeId);
        task.setUpdatedOn(LocalDateTime.now());
        taskRepository.save(task);

        if(task.getStatus()!=TaskStatus.completed)
            kafkaPublisher.publishTaskToNode(nodeId, taskId);

        return ResponseEntity.status(HttpStatus.OK).body(task);
    }

    public ResponseEntity<?> reassignAllTaskToDifferentNode(String oldNodeId, String newNodeId){
        List<Task> taskList=taskRepository.findAllByAssignedNodeIdAndStatusNot(oldNodeId,TaskStatus.completed);

        taskList.forEach(task -> {
            task.setStatus(TaskStatus.pending);
            task.setAssignedNodeId(newNodeId);
            task.setUpdatedOn(LocalDateTime.now());
        });

        taskRepository.saveAll(taskList);

        taskList.forEach(task -> {
            if(task.getStatus()!=TaskStatus.completed)
                kafkaPublisher.publishTaskToNode(newNodeId, task.getTaskId());
        });

        return ResponseEntity.status(HttpStatus.OK).body("OK");
    }

    public ResponseEntity<?> updateStatus(String taskId, String oldStatus, String newStatus){
        if(TaskStatus.valueOf(oldStatus)==TaskStatus.completed)
            return ResponseEntity.status(HttpStatus.OK).body("Invalid status update");

        Task task=taskRepository.findByTaskId(taskId).orElseThrow();

        if(task.getStatus()==TaskStatus.completed)
            return ResponseEntity.status(HttpStatus.OK).body("Invalid status update");

        task.setStatus(TaskStatus.valueOf(newStatus));
        task.setUpdatedOn(LocalDateTime.now());
        taskRepository.save(task);

        return ResponseEntity.status(HttpStatus.OK).body(task);
    }

}
