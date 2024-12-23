package my.project.qri3a.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.AuthenticationRequest;
import my.project.qri3a.dtos.requests.UserRequestDTO;
import my.project.qri3a.dtos.responses.AuthenticationResponse;
import my.project.qri3a.entities.TokenModel;
import my.project.qri3a.entities.User;
import my.project.qri3a.enums.Role;
import my.project.qri3a.enums.TokenType;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;
import my.project.qri3a.mappers.UserMapper;
import my.project.qri3a.repositories.TokenRepository;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.AuthenticationService;
import my.project.qri3a.services.JwtService;
import my.project.qri3a.services.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Override
    public AuthenticationResponse registerUser(UserRequestDTO request) throws ResourceAlreadyExistsException {

        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new ResourceAlreadyExistsException("User With email : "+request.getEmail()+" already exists.");
        });

        User userToRegister = userMapper.toEntity(request);
        User savedUser  = userRepository.save(userToRegister);

        savedUser.setName("");
        savedUser.setAddress("");
        savedUser.setLocation("");
        savedUser.setPhoneNumber("");
        savedUser.setRole(Role.SELLER);
        savedUser.setRating(5F);

        var jwtToken = jwtService.generateToken((UserDetails) savedUser);
        var refreshToken = jwtService.generateRefreshToken((UserDetails) savedUser);

        saveUserToken(savedUser, jwtToken);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
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
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        var jwtToken = jwtService.generateToken((UserDetails) user);
        var refreshToken = jwtService.generateRefreshToken((UserDetails) user);
        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .id(user.getId())
                .build();

    }

    private void saveUserToken(User user, String jwtToken) {
        var token = TokenModel.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;
        if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
            return;
        }
        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow();
            if (jwtService.isTokenValid(refreshToken, (UserDetails) user)) {
                var accessToken = jwtService.generateToken((UserDetails) user);
                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);
                var authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }


}
