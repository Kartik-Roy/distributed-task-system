package task.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import task.server.dto.CreateUserDto;
import task.server.entity.User;
import task.server.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();


    public ResponseEntity<?> create(CreateUserDto createUserDto){
        if(userRepository.findByUsername(createUserDto.getUsername()).isPresent())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username is taken");

        User user=User.builder()
                .username(createUserDto.getUsername())
                .passwordHash(encoder.encode(createUserDto.getPassword()))
                .active(true)
                .role(createUserDto.getRole())
                .build();
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

}
