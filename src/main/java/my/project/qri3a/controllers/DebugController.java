package my.project.qri3a.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur temporaire pour déboguer les problèmes d'autorisation
 * À supprimer en production
 */
@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    @GetMapping("/auth-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuthInfo(Authentication authentication) {
        Map<String, Object> authInfo = new HashMap<>();

        if (authentication != null) {
            authInfo.put("isAuthenticated", authentication.isAuthenticated());
            authInfo.put("principal", authentication.getPrincipal().toString());
            authInfo.put("name", authentication.getName());
            authInfo.put("authorities", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
            authInfo.put("details", authentication.getDetails() != null ?
                    authentication.getDetails().toString() : null);
        } else {
            authInfo.put("isAuthenticated", false);
        }

        log.info("Auth debug info: {}", authInfo);

        ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                authInfo,
                "Authentication information retrieved",
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }
}