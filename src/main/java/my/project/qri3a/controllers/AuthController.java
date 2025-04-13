package my.project.qri3a.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.AuthenticationRequest;
import my.project.qri3a.dtos.requests.EmailAndPasswordDTO;
import my.project.qri3a.dtos.responses.ApiResponseDto;
import my.project.qri3a.dtos.responses.AuthenticationResponse;
import my.project.qri3a.exceptions.InvalidCredentialsException;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.responses.ApiResponse;
import my.project.qri3a.services.AuthenticationService;
import my.project.qri3a.services.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    private final JwtService jwtService;

    /**
     * Endpoint pour enregistrer un utilisateur avec email et mot de passe.
     *
     * @param request  Les informations d'inscription de l'utilisateur.
     * @param response La réponse HTTP pour ajouter les cookies.
     * @return Une réponse JSON standardisée.
     * @throws IOException En cas d'erreur d'entrée/sortie.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> registerUserByEmailAndPassword(
            @Valid @RequestBody EmailAndPasswordDTO request,
            HttpServletResponse response
    ) throws IOException {
        // Encoder le mot de passe avant l'inscription

        AuthenticationResponse authResponse = authenticationService.registerUser(request);

        // Construire la réponse JSON qui inclut l'AuthenticationResponse
        ApiResponse<AuthenticationResponse> apiResponse = new ApiResponse<>(
                authResponse,
                "Utilisateur enregistré avec succès.",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Endpoint pour authentifier un utilisateur.
     *
     * @param request  Les informations d'authentification de l'utilisateur.
     * @param response La réponse HTTP pour ajouter les cookies.
     * @return Une réponse JSON standardisée.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto> authenticate(
            @Valid @RequestBody AuthenticationRequest request,
            HttpServletResponse response
    ) throws InvalidCredentialsException, ResourceNotFoundException {
        AuthenticationResponse authResponse = authenticationService.authenticate(request);

        // Définir le token d'accès comme cookie HttpOnly
        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", authResponse.getAccessToken())
                .httpOnly(true)
                .secure(false) // Mettre à true en production
                .path("/")
                .maxAge(jwtService.getJwtExpiration() / 1000) // Expiration du token d'accès
                .sameSite("Lax") // Options : "Strict", "Lax", "None"
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        // Définir le token de rafraîchissement comme cookie HttpOnly
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", authResponse.getRefreshToken())
                .httpOnly(true)
                .secure(false) // Mettre à true en production
                .path("/")
                .maxAge(jwtService.getRefreshExpiration() / 1000) // Convertir les millisecondes en secondes
                .sameSite("Strict") // Utiliser "Strict" ou "Lax"
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // Construire la réponse JSON
        ApiResponseDto apiResponse = new ApiResponseDto("SUCCESS", "Utilisateur authentifié avec succès.");
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Endpoint pour rafraîchir les tokens d'authentification.
     *
     * @param request  La requête HTTP contenant les cookies.
     * @param response La réponse HTTP pour ajouter les nouveaux cookies.
     * @return Une réponse JSON standardisée.
     * @throws IOException En cas d'erreur d'entrée/sortie.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponseDto> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        // Lire le Refresh Token depuis le cookie HttpOnly
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            ApiResponseDto apiResponse = new ApiResponseDto("ERROR", "Refresh token non trouvé.");
            return ResponseEntity.status(401).body(apiResponse);
        }

        // Tenter de trouver le cookie refresh_token
        String refreshToken = Arrays.stream(cookies)
                .filter(cookie -> "refresh_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (refreshToken == null) {
            ApiResponseDto apiResponse = new ApiResponseDto("ERROR", "Refresh token non trouvé.");
            return ResponseEntity.status(401).body(apiResponse);
        }

        // Déléguer au service pour valider et générer un nouveau access token
        AuthenticationResponse newAuthResponse = authenticationService.refreshToken(refreshToken);

        if (newAuthResponse.getAccessToken() == null) {
            ApiResponseDto apiResponse = new ApiResponseDto("ERROR", "Refresh token invalide.");
            return ResponseEntity.status(403).body(apiResponse);
        }

        // Définir le nouveau token d'accès comme cookie HttpOnly
        ResponseCookie newAccessTokenCookie = ResponseCookie.from("access_token", newAuthResponse.getAccessToken())
                .httpOnly(true)
                .secure(false) // Mettre à true en production
                .path("/")
                .maxAge(jwtService.getJwtExpiration() / 1000) // Utiliser la bonne expiration pour le access token
                .sameSite("Lax") // Options : "Strict", "Lax", "None"
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, newAccessTokenCookie.toString());

        // Ne pas définir un nouveau refresh token

        // Construire la réponse JSON
        ApiResponseDto apiResponse = new ApiResponseDto("SUCCESS", "Access token rafraîchi avec succès.");
        return ResponseEntity.ok(apiResponse);
    }



}