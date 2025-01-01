package my.project.qri3a.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.AuthenticationRequest;
import my.project.qri3a.dtos.requests.EmailAndPasswordDTO;
import my.project.qri3a.dtos.requests.UserRequestDTO;
import my.project.qri3a.dtos.responses.AuthenticationResponse;
import my.project.qri3a.entities.User;
import my.project.qri3a.enums.Role;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.mappers.UserMapper;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.AuthenticationService;
import my.project.qri3a.services.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Override
    public AuthenticationResponse registerUser(EmailAndPasswordDTO request) throws ResourceAlreadyExistsException {

        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new ResourceAlreadyExistsException("User with email: " + request.getEmail() + " already exists.");
        });

        User userToRegister = new User();
        userToRegister.setEmail(request.getEmail());
        userToRegister.setPassword(request.getPassword());
        // Initialize default user attributes
        userToRegister.setName("");
        userToRegister.setAddress("");
        userToRegister.setCity("");
        userToRegister.setPhoneNumber("");
        userToRegister.setRole(Role.SELLER);
        userToRegister.setRating(5F);
        User savedUser = userRepository.save(userToRegister);

        // Generate tokens
        String accessToken = jwtService.generateToken((UserDetails) savedUser);
        String refreshToken = jwtService.generateRefreshToken((UserDetails) savedUser);

        // No token saving

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role("SELLER")
                .id(userToRegister.getId())
                .build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String accessToken = jwtService.generateToken((UserDetails) user);
        String refreshToken = jwtService.generateRefreshToken((UserDetails) user);

        // No token revocation since tokens aren't stored

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .id(user.getId())
                .build();
    }

    @Override
    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow();
            if (jwtService.isTokenValid(refreshToken, (UserDetails) user)) {
                var accessToken = jwtService.generateToken((UserDetails) user);
                var authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken) // Optionally generate a new refresh token
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }
}
