package my.project.qri3a.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.services.JwtService;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Extract Access Token from Cookies
        String accessToken = null;
        if (request.getCookies() != null) {
            accessToken = Arrays.stream(request.getCookies())
                    .filter(cookie -> "access_token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        if (accessToken == null || accessToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract user email from token
            String userEmail = jwtService.extractEmailFromToken(accessToken);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Log pour déboguer les autorités
                log.debug("User {} has authorities: {}", userEmail,
                        userDetails.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.joining(", ")));



                if (jwtService.isTokenValid(accessToken, userDetails)) {
                    // Vérifier si l'utilisateur est bloqué
                    if (!userDetails.isEnabled() && isProtectedEndpoint(request)) {
                        throw new DisabledException("User account is blocked");
                    }
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities() // S'assurer que les autorités sont transmises
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Log de l'authentification réussie avec les rôles
                    log.debug("Authentication successful for user {} with roles: {}", userEmail,
                            userDetails.getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .collect(Collectors.joining(", ")));
                }
            }
        } catch (DisabledException e) {
            log.error("User account is blocked: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"message\":\"" + e.getMessage() + "\",\"status\":" + HttpServletResponse.SC_FORBIDDEN + "}");
            response.getWriter().flush();
            return;
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }


    /**
     * Détermine si l'endpoint est protégé (nécessite une authentification)
     */
    private boolean isProtectedEndpoint(HttpServletRequest request) {
        String path = request.getServletPath();

        // Liste des chemins publics (non protégés)
        return !(
                path.startsWith("/api/v1/auth/") ||
                        (path.startsWith("/api/v1/products") && request.getMethod().equals("GET") && !path.contains("/my")) ||
                        (path.startsWith("/api/v1/reviews") && request.getMethod().equals("GET")) ||
                        (path.startsWith("/api/v1/favorites") && request.getMethod().equals("GET")) ||
                        path.startsWith("/api/v1/public/") ||
                        path.matches("/api/v1/users/seller-profile/\\d+") ||
                        path.startsWith("/swagger-ui/") ||
                        path.startsWith("/v3/api-docs/") ||
                        path.equals("/swagger-ui.html") ||
                        path.startsWith("/swagger-resources/") ||
                        path.startsWith("/webjars/") ||
                        path.equals("/docs") ||
                        path.equals("/api-docs/swagger-config") ||
                        path.equals("/api-docs")
        );
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/v1/auth/") &&
                !path.equals("/api/v1/auth/refresh-token") &&
                !path.equals("/api/v1/auth/logout");
    }
}
