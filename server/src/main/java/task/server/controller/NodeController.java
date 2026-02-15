package task.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import task.server.dto.CreateNodeDto;
import task.server.service.NodeService;

@RestController
@RequestMapping("/node")
public class NodeController {

    @Autowired
    private NodeService nodeService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody CreateNodeDto createNodeDto){
        return nodeService.create(createNodeDto);
    }
}
