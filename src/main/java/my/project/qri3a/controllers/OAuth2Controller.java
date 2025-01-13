package my.project.qri3a.controllers;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.dtos.TokenDto;
import my.project.qri3a.dtos.UrlDto;
import my.project.qri3a.dtos.responses.AuthenticationResponse;
import my.project.qri3a.entities.User;
import my.project.qri3a.enums.Role;
import my.project.qri3a.repositories.UserRepository;
import my.project.qri3a.services.AuthenticationService;
import my.project.qri3a.services.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class OAuth2Controller {

    @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-secret}")
    private String clientSecret;

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @GetMapping("/url")
    public ResponseEntity<UrlDto> auth() {
        String url = new GoogleAuthorizationCodeRequestUrl(clientId,
                "http://localhost:4200",
                Arrays.asList(
                        "email",
                        "profile",
                        "openid")).build();

        return ResponseEntity.ok(new UrlDto(url));
    }

    @GetMapping("/callback")
    public ResponseEntity<AuthenticationResponse> callback(@RequestParam("code") String code) {

        String token;
        User user;
        String accessToken, refreshToken;
        try {
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    clientId,
                    clientSecret,
                    code,
                    "http://localhost:4200" // Updated Redirect URI
            ).execute();

            token = tokenResponse.getAccessToken();

            OAuth2AuthenticatedPrincipal principal = jwtService.getPrincipalFromToken(token);
            String email = principal.getAttribute("email");
            String name = principal.getAttribute("name");

            user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setRating(5F);
                newUser.setAddress("");
                newUser.setCity("");
                newUser.setPhoneNumber("");
                newUser.setName(name);
                newUser.setPassword(passwordEncoder.encode("00000000")); // Consider revising
                newUser.setRole(Role.SELLER);
                return userRepository.save(newUser);
            });

            // Ensure User implements UserDetails or adjust jwtService methods
            accessToken = jwtService.generateToken((UserDetails) user);
            refreshToken = jwtService.generateRefreshToken((UserDetails) user);

        } catch (IOException e) {
            log.error("Error during Google OAuth callback: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .id(user.getId())
                .build();

        return ResponseEntity.ok(authResponse);
    }
}