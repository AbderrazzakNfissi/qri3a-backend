package my.project.qri3a.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
@RequiredArgsConstructor
@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;
    @Getter
    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;
    private final WebClient userInfoClient;

    public String extractEmailFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver){
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);

    }

    private Claims extractAllClaims(String token){
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] key = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(key);
    }

    //constructs a jwt token with claims, subject, issued at and signs it with the secret key
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ){
        return buildToken(extraClaims, userDetails, jwtExpiration);

    }

    //Generate Token without extra claims
    public String generateToken(UserDetails userDetails){
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateRefreshToken(
            UserDetails userDetails
    ){
        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
    }

    public boolean isTokenValid(String token, UserDetails userDetails){
        final String username = extractEmailFromToken(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public OAuth2AuthenticatedPrincipal getPrincipalFromToken(String token) {
        UserInfo userInfo = userInfoClient.get()
                .uri( uriBuilder -> uriBuilder
                        .path("/oauth2/v3/userinfo")
                        .queryParam("access_token", token)
                        .build())
                .retrieve()
                .bodyToMono(UserInfo.class)
                .block();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", userInfo.sub());
        attributes.put("email", userInfo.email());
        attributes.put("name", userInfo.name());
        attributes.put("picture",userInfo.picture());
        return new OAuth2IntrospectionAuthenticatedPrincipal(userInfo.name(), attributes, null);
    }
}
