package my.project.qri3a.services.impl;

import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.AuthenticationRequest;
import my.project.qri3a.dtos.requests.EmailAndPasswordDTO;
import my.project.qri3a.dtos.responses.AuthenticationResponse;
import my.project.qri3a.entities.User;
import my.project.qri3a.enums.Role;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.AuthenticationService;
import my.project.qri3a.services.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthenticationResponse registerUser(EmailAndPasswordDTO request) throws ResourceAlreadyExistsException {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new ResourceAlreadyExistsException("User with email: " + request.getEmail() + " already exists.");
        });

        User userToRegister = new User();
        userToRegister.setEmail(request.getEmail());
        userToRegister.setPassword(request.getPassword());
        userToRegister.setName("");
        userToRegister.setAddress("");
        userToRegister.setCity("");
        userToRegister.setPhoneNumber("");
        userToRegister.setRole(Role.SELLER);
        userToRegister.setRating(5F);
        User savedUser = userRepository.save(userToRegister);

        // Generate tokens
        String accessToken = jwtService.generateToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        // Return both tokens (caller will set them in HttpOnly cookies and remove from body)
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
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .id(user.getId())
                .build();
    }

    /**
     * Updated to handle token rotation.
     * Generates new Access and Refresh Tokens.
     */
    @Override
    public AuthenticationResponse refreshToken(String refreshToken) {
        // Validate refresh token
        String userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail == null) {
            return AuthenticationResponse.builder().build(); // Invalid token
        }

        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null || !jwtService.isTokenValid(refreshToken, user)) {
            return AuthenticationResponse.builder().build(); // Invalid token
        }

        // Generate new tokens
        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        // Optionally, implement token revocation or persistence to track active refresh tokens

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
