package my.project.qri3a.controllers;

import lombok.RequiredArgsConstructor;
import my.project.qri3a.config.UserAuthProvider;
import my.project.qri3a.dtos.requests.CredentialsDTO;
import my.project.qri3a.dtos.responses.LoggedInDTO;
import my.project.qri3a.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService userService;
    private final UserAuthProvider userAuthProvider;

    @PostMapping("/login")
    public ResponseEntity<LoggedInDTO> login(@RequestBody CredentialsDTO credentials) {
        LoggedInDTO loggedInUser = userService.login(credentials);
        loggedInUser.setToken(userAuthProvider.createToken(loggedInUser));
        return ResponseEntity.ok(loggedInUser);
    }

    @PostMapping("/register")
    public ResponseEntity<LoggedInDTO> register(@RequestBody CredentialsDTO credentials) {
        LoggedInDTO createdUser = userService.register(credentials);
        createdUser.setToken(userAuthProvider.createToken(createdUser));
        return ResponseEntity.created(URI.create("/api/v1/users/"+createdUser.getId())).body(createdUser);
    }
}
