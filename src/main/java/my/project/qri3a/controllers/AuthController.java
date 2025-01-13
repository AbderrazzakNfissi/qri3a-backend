package my.project.qri3a.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.AuthenticationRequest;
import my.project.qri3a.dtos.requests.EmailAndPasswordDTO;
import my.project.qri3a.dtos.responses.AuthenticationResponse;
import my.project.qri3a.services.AuthenticationService;
import my.project.qri3a.services.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> registerUserByEmailAndPassword(
            @Valid @RequestBody EmailAndPasswordDTO request,
            HttpServletResponse response
    ) throws IOException {
        // Encode the password before registration
        request.setPassword(passwordEncoder.encode(request.getPassword()));
        AuthenticationResponse authResponse = authenticationService.registerUser(request);

        // Set Refresh Token as HttpOnly Cookie
        //Cookie refreshTokenCookie = new Cookie("refresh_token", authResponse.getRefreshToken());
        //refreshTokenCookie.setHttpOnly(true);
        //refreshTokenCookie.setSecure(true); // Set to true in production (requires HTTPS)
        //refreshTokenCookie.setPath("/api/v1/auth/refresh-token"); // Set path to restrict where the cookie is sent
        //refreshTokenCookie.setMaxAge((int) (jwtService.getRefreshExpiration() / 1000)); // Convert milliseconds to seconds
        //response.addCookie(refreshTokenCookie);

        // Optionally remove refresh token from response body for security
        // authResponse.setRefreshToken(null);

        return ResponseEntity.ok(authResponse);
    }


    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request,
            HttpServletResponse response
    ){
        AuthenticationResponse authResponse = authenticationService.authenticate(request);

        // Set Refresh Token as HttpOnly Cookie
        //Cookie refreshTokenCookie = new Cookie("refresh_token", authResponse.getRefreshToken());
        //refreshTokenCookie.setHttpOnly(true);
        //refreshTokenCookie.setSecure(true); // Set to true in production (requires HTTPS)
        //refreshTokenCookie.setPath("/api/v1/auth/refresh-token"); // Set path to restrict where the cookie is sent
        //refreshTokenCookie.setMaxAge((int) (jwtService.getRefreshExpiration() / 1000)); // Convert milliseconds to seconds
        //response.addCookie(refreshTokenCookie);

        // Optionally remove refresh token from response body for security
        //authResponse.setRefreshToken(null);

        return ResponseEntity.ok(authResponse);
    }


    @PostMapping("/refresh-token")
    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        authenticationService.refreshToken(request, response);
    }


}
