package task.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import task.server.dto.NodeLoginRequestDto;
import task.server.dto.UserLoginRequestDto;
import task.server.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequestDto userLoginRequestDto) {
        return authService.login(userLoginRequestDto);
    }

    @PostMapping(value = "/login/node")
    public ResponseEntity<?> loginNode(@RequestBody NodeLoginRequestDto nodeLoginRequestDto) {
        return authService.loginNode(nodeLoginRequestDto);
    }
}

