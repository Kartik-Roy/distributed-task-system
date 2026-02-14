package task.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import task.server.dto.CreateTaskDto;
import task.server.service.TaskService;

@RestController
@RequestMapping("/task")
public class TaskController {

    @Autowired
    private TaskService taskService;


    @PostMapping("/create")
    public ResponseEntity<?> createTask(@RequestBody CreateTaskDto createTaskDto){
        return taskService.createTask(createTaskDto);
    }

    @GetMapping("/getByTaskId")
    public ResponseEntity<?> getTaskByTaskId(@RequestParam String taskId){
        return taskService.getTaskByTaskId(taskId);
    }

    @GetMapping("/getAllForNode")
    public ResponseEntity<?> getAllTaskForNode(){
        return taskService.getAllTaskForNode();
    }

    @PutMapping("/reassignTask")
    public ResponseEntity<?> reassignTaskToDifferentNode(@RequestParam String taskId, @RequestParam String nodeId){
        return taskService.reassignTaskToDifferentNode(taskId, nodeId);
    }

    @PutMapping("/reassignAllForNode")
    public ResponseEntity<?> reassignAllTaskToDifferentNode(@RequestParam String oldNodeId, @RequestParam String newNodeId){
        return taskService.reassignAllTaskToDifferentNode(oldNodeId,newNodeId);
    }

    @PutMapping("/updateStatus")
    public ResponseEntity<?> updateStatus(@RequestParam String taskId, @RequestParam String oldStatus, @RequestParam String newStatus){
        return taskService.updateStatus(taskId, oldStatus, newStatus);
    }

}
