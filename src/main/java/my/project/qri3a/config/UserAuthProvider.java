package my.project.qri3a.config;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import my.project.qri3a.dtos.requests.CredentialsDTO;
import my.project.qri3a.dtos.responses.LoggedInDTO;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.mappers.UserMapper;
import my.project.qri3a.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@RequiredArgsConstructor
@Component
public class UserAuthProvider {

    @Value("${security.jwt.token.secret-key:secret-key}")
    private String secretKey;
    private UserRepository userRepository;
    private UserMapper userMapper;
    @PostConstruct
    protected void init(){
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public String createToken(LoggedInDTO user){
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 1000 * 60 * 60 * 24);
        return JWT.create()
                .withIssuer(user.getEmail())
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .withClaim("email",user.getEmail())
                .sign(Algorithm.HMAC256(secretKey));
    }

    public Authentication validateToken(String token){
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        JWTVerifier verifier =  JWT.require(algorithm).build();

        DecodedJWT decoded = verifier.verify(token);

        LoggedInDTO loggedInDTO =  LoggedInDTO.builder()
                .email(decoded.getIssuer())
                .email(decoded.getClaim("email").asString())
                .build();

        return new UsernamePasswordAuthenticationToken(loggedInDTO,null, Collections.emptyList());
    }

    public Authentication validateTokenStrongly(String token){
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        JWTVerifier verifier =  JWT.require(algorithm).build();

        DecodedJWT decoded = verifier.verify(token);
        User user = userRepository.findByEmail(decoded.getIssuer())
                      .orElseThrow(()->new ResourceNotFoundException("Unkonwn User "));


        LoggedInDTO loggedInDTO =  userMapper.toLoggedInDTO(user);

        return new UsernamePasswordAuthenticationToken(loggedInDTO,null, Collections.emptyList());
    }
}
